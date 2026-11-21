data "aws_ssm_parameter" "ecs_ami" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2023/recommended/image_id"
}

resource "aws_ecs_cluster" "main" {
  name = local.name_prefix

  setting {
    name  = "containerInsights"
    value = "disabled"
  }

  tags = local.common_tags
}

resource "aws_cloudwatch_log_group" "ecs" {
  for_each = toset(["frontend", "jakarta"])

  name              = "/ecs/${local.name_prefix}/${each.key}"
  retention_in_days = 7

  tags = local.common_tags
}

resource "aws_service_discovery_private_dns_namespace" "main" {
  name = "smarttrans.local"
  vpc  = aws_vpc.main.id

  tags = local.common_tags
}

resource "aws_service_discovery_service" "jakarta" {
  name = "smarttrans-jakarta"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }

  tags = local.common_tags
}

resource "aws_launch_template" "ecs" {
  name          = local.name_prefix
  image_id      = data.aws_ssm_parameter.ecs_ami.value
  instance_type = "t3.small"

  iam_instance_profile {
    arn = aws_iam_instance_profile.ecs.arn
  }

  vpc_security_group_ids = [aws_security_group.ecs.id]

  user_data = base64encode(<<-EOF
    #!/bin/bash
    echo ECS_CLUSTER=${aws_ecs_cluster.main.name} >> /etc/ecs/ecs.config
  EOF
  )

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_size           = 30
      volume_type           = "gp3"
      encrypted             = true
      delete_on_termination = true
    }
  }

  tag_specifications {
    resource_type = "instance"
    tags = merge(local.common_tags, {
      Name = "${local.name_prefix}-ecs"
    })
  }

  tags = local.common_tags
}

resource "aws_autoscaling_group" "ecs" {
  name                = local.name_prefix
  vpc_zone_identifier = [for subnet in aws_subnet.private : subnet.id]
  min_size            = 1
  max_size            = 1
  desired_capacity    = 1
  health_check_type   = "EC2"

  launch_template {
    id      = aws_launch_template.ecs.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "${local.name_prefix}-ecs"
    propagate_at_launch = true
  }

  dynamic "tag" {
    for_each = local.common_tags
    content {
      key                 = tag.key
      value               = tag.value
      propagate_at_launch = true
    }
  }
}

resource "aws_ecs_task_definition" "jakarta" {
  family                   = "${local.name_prefix}-jakarta"
  network_mode             = "awsvpc"
  requires_compatibilities = ["EC2"]
  execution_role_arn       = aws_iam_role.ecs_execution.arn

  container_definitions = jsonencode([
    {
      name      = "jakarta"
      image     = "${aws_ecr_repository.app["smarttrans-jakarta"].repository_url}:latest"
      essential = true
      cpu       = 1024
      memory    = 1536
      portMappings = [
        {
          containerPort = local.application_ports.jakarta
          hostPort      = local.application_ports.jakarta
          protocol      = "tcp"
        }
      ]
      environment = [
        {
          name  = "WILDFLY_JAVA_OPTS"
          value = "-Djboss.bind.address=0.0.0.0 -Xms512m -Xmx768m -XX:MaxMetaspaceSize=256m"
        }
      ]
      secrets = [
        {
          name      = "POSTGRESQL_URL"
          valueFrom = "${aws_secretsmanager_secret.db.arn}:jdbc_url::"
        },
        {
          name      = "POSTGRESQL_USER"
          valueFrom = "${aws_secretsmanager_secret.db.arn}:username::"
        },
        {
          name      = "POSTGRESQL_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.db.arn}:password::"
        },
        {
          name      = "JWT_SECRET"
          valueFrom = aws_secretsmanager_secret.jwt.arn
        },
        {
          name      = "MISTRAL_API_KEY"
          valueFrom = local.mistral_api_key_secret_arn
        }
      ]
      healthCheck = {
        command     = ["CMD-SHELL", "bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf \"GET /actuator/health HTTP/1.0\\r\\n\\r\\n\" >&3 && grep -q UP <&3'"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 240
      }
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs["jakarta"].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_task_definition" "frontend" {
  family                   = "${local.name_prefix}-frontend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["EC2"]
  execution_role_arn       = aws_iam_role.ecs_execution.arn

  container_definitions = jsonencode([
    {
      name      = "frontend"
      image     = "${aws_ecr_repository.app["smarttrans-frontend"].repository_url}:latest"
      essential = true
      cpu       = 256
      memory    = 256
      portMappings = [
        {
          containerPort = local.application_ports.frontend
          hostPort      = local.application_ports.frontend
          protocol      = "tcp"
        }
      ]
      environment = [
        {
          name  = "API_UPSTREAM"
          value = "smarttrans-jakarta.smarttrans.local:8080"
        }
      ]
      healthCheck = {
        command     = ["CMD-SHELL", "wget -q -T 4 -O - http://127.0.0.1/actuator/health | grep -q UP"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 240
      }
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs["frontend"].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_service" "jakarta" {
  name            = "${local.name_prefix}-jakarta"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.jakarta.arn
  desired_count   = 1
  launch_type     = "EC2"

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  network_configuration {
    subnets          = [for subnet in aws_subnet.private : subnet.id]
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.jakarta.arn
  }

  depends_on = [aws_secretsmanager_secret_version.db, aws_secretsmanager_secret_version.jwt]

  # GitHub Actions publishes new revisions. Keep the deployed revision.
  lifecycle {
    ignore_changes = [task_definition]
  }

  tags = local.common_tags
}

resource "aws_ecs_service" "frontend" {
  name            = "${local.name_prefix}-frontend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.frontend.arn
  desired_count   = 1
  launch_type     = "EC2"

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100
  health_check_grace_period_seconds  = 300

  network_configuration {
    subnets          = [for subnet in aws_subnet.private : subnet.id]
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.frontend.arn
    container_name   = "frontend"
    container_port   = local.application_ports.frontend
  }

  depends_on = [aws_lb_listener.http, aws_ecs_service.jakarta]

  # GitHub Actions publishes new revisions. Keep the deployed revision.
  lifecycle {
    ignore_changes = [task_definition]
  }

  tags = local.common_tags
}
