#!/usr/bin/env bash
# Scale the dev runtime down without deleting Terraform-managed resources.
# The next start restores the capacities configured here. A later terraform
# apply also restores the Terraform desired counts.
#
# These resources keep billing while the runtime is stopped:
# NAT Gateway, its public IPv4 address, the Application Load Balancer and its
# public IPv4 addresses, ECR storage, Secrets Manager, both Route 53 zones,
# CloudFront, RDS storage, and retained CloudWatch logs.
# Scaling the group to zero terminates the EC2 instance. Its root volume is
# deleted with the instance. AWS may automatically restart a stopped RDS
# instance after seven days.

set -euo pipefail

REGION="${AWS_REGION:-eu-west-3}"
CLUSTER="smarttrans-dev"
ASG="smarttrans-dev"
RDS="smarttrans-dev"
FRONTEND="smarttrans-dev-frontend"
JAKARTA="smarttrans-dev-jakarta"

die() {
  echo "error: $*" >&2
  exit 1
}

service_counts() {
  aws ecs describe-services --region "$REGION" --cluster "$CLUSTER" --services "$1" \
    --query 'services[0].[desiredCount,runningCount,pendingCount]' --output text
}

wait_service_idle() {
  local service="$1"
  local attempt
  for attempt in $(seq 1 60); do
    local counts desired running pending
    counts="$(service_counts "$service")"
    read -r desired running pending <<< "$counts"
    if [[ "$desired" == "0" && "$running" == "0" && "$pending" == "0" ]]; then
      echo "$service is stopped"
      return 0
    fi
    sleep 10
  done
  die "$service did not reach zero tasks"
}

scale_service() {
  local service="$1"
  aws ecs update-service --region "$REGION" --cluster "$CLUSTER" --service "$service" \
    --desired-count 0 >/dev/null
  wait_service_idle "$service"
}

echo "Stopping frontend tasks"
scale_service "$FRONTEND"
echo "Stopping Jakarta tasks"
scale_service "$JAKARTA"

echo "Removing EC2 capacity from $ASG"
aws autoscaling update-auto-scaling-group --region "$REGION" \
  --auto-scaling-group-name "$ASG" --min-size 0 --desired-capacity 0

for attempt in $(seq 1 60); do
  instances="$(aws autoscaling describe-auto-scaling-groups --region "$REGION" \
    --auto-scaling-group-names "$ASG" --query 'length(AutoScalingGroups[0].Instances)' --output text)"
  if [[ "$instances" == "0" ]]; then
    echo "EC2 capacity is stopped"
    break
  fi
  if [[ "$attempt" == "60" ]]; then
    die "EC2 instances are still registered in $ASG"
  fi
  sleep 10
done

db_status="$(aws rds describe-db-instances --region "$REGION" --db-instance-identifier "$RDS" \
  --query 'DBInstances[0].DBInstanceStatus' --output text)"
case "$db_status" in
  available)
    echo "Stopping RDS instance $RDS"
    aws rds stop-db-instance --region "$REGION" --db-instance-identifier "$RDS" >/dev/null
    ;;
  stopped|stopping)
    echo "RDS instance $RDS is already $db_status"
    ;;
  *)
    die "RDS instance $RDS is $db_status and cannot be stopped safely"
    ;;
esac

for attempt in $(seq 1 60); do
  db_status="$(aws rds describe-db-instances --region "$REGION" --db-instance-identifier "$RDS" \
    --query 'DBInstances[0].DBInstanceStatus' --output text)"
  if [[ "$db_status" == "stopped" ]]; then
    echo "RDS instance is stopped"
    exit 0
  fi
  if [[ "$db_status" != "stopping" && "$db_status" != "available" ]]; then
    die "RDS instance entered unexpected state $db_status"
  fi
  sleep 15
done
die "RDS instance did not stop"
