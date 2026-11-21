#!/usr/bin/env bash
# Restore the dev runtime in dependency order: RDS, EC2 capacity, Jakarta, frontend.
# Desired counts match the Terraform configuration: one instance and one task
# for each service. The script does not delete resources.

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

wait_service_running() {
  local service="$1"
  local attempt
  for attempt in $(seq 1 90); do
    local counts desired running
    counts="$(service_counts "$service")"
    read -r desired running _pending <<< "$counts"
    if [[ "$desired" == "1" && "$running" -ge 1 ]]; then
      echo "$service is running"
      return 0
    fi
    sleep 10
  done
  die "$service did not reach a running task"
}

db_status="$(aws rds describe-db-instances --region "$REGION" --db-instance-identifier "$RDS" \
  --query 'DBInstances[0].DBInstanceStatus' --output text)"
case "$db_status" in
  stopped)
    echo "Starting RDS instance $RDS"
    aws rds start-db-instance --region "$REGION" --db-instance-identifier "$RDS" >/dev/null
    ;;
  available|starting|backing-up|configuring|modifying)
    echo "RDS instance $RDS is already $db_status"
    ;;
  *)
    die "RDS instance $RDS is $db_status and cannot be started safely"
    ;;
esac

for attempt in $(seq 1 80); do
  db_status="$(aws rds describe-db-instances --region "$REGION" --db-instance-identifier "$RDS" \
    --query 'DBInstances[0].DBInstanceStatus' --output text)"
  if [[ "$db_status" == "available" ]]; then
    echo "RDS instance is available"
    break
  fi
  if [[ "$attempt" == "80" ]]; then
    die "RDS instance did not become available (last state: $db_status)"
  fi
  sleep 15
done

echo "Restoring EC2 capacity"
aws autoscaling update-auto-scaling-group --region "$REGION" \
  --auto-scaling-group-name "$ASG" --min-size 1 --desired-capacity 1

for attempt in $(seq 1 60); do
  in_service="$(aws autoscaling describe-auto-scaling-groups --region "$REGION" \
    --auto-scaling-group-names "$ASG" \
    --query 'length(AutoScalingGroups[0].Instances[?LifecycleState==`InService`])' --output text)"
  if [[ "$in_service" -ge 1 ]]; then
    echo "EC2 instance is in service"
    break
  fi
  if [[ "$attempt" == "60" ]]; then
    die "No EC2 instance became InService"
  fi
  sleep 10
done

for attempt in $(seq 1 60); do
  active="$(aws ecs list-container-instances --region "$REGION" --cluster "$CLUSTER" --status ACTIVE \
    --query 'length(containerInstanceArns)' --output text)"
  if [[ "$active" -ge 1 ]]; then
    echo "ECS container instance is active"
    break
  fi
  if [[ "$attempt" == "60" ]]; then
    die "ECS container instance did not register"
  fi
  sleep 10
done

echo "Starting Jakarta"
aws ecs update-service --region "$REGION" --cluster "$CLUSTER" --service "$JAKARTA" \
  --desired-count 1 >/dev/null
wait_service_running "$JAKARTA"

echo "Starting frontend"
aws ecs update-service --region "$REGION" --cluster "$CLUSTER" --service "$FRONTEND" \
  --desired-count 1 >/dev/null
wait_service_running "$FRONTEND"

echo "Dev runtime is started"
