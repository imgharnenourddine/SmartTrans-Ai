locals {
  application_ports = {
    frontend = 80
    jakarta  = 8080
  }
}

resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-alb"
  description = "Acces HTTP et HTTPS public vers l'ALB"
  vpc_id      = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-alb"
  })
}

resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id
  description       = "HTTP depuis Internet"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "alb_https" {
  security_group_id = aws_security_group.alb.id
  description       = "HTTPS depuis Internet"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "alb_to_ecs" {
  for_each = local.application_ports

  security_group_id            = aws_security_group.alb.id
  description                  = "Trafic applicatif vers ECS (${each.key})"
  referenced_security_group_id = aws_security_group.ecs.id
  from_port                    = each.value
  to_port                      = each.value
  ip_protocol                  = "tcp"
}

resource "aws_security_group" "ecs" {
  name        = "${local.name_prefix}-ecs"
  description = "Trafic applicatif uniquement depuis l'ALB"
  vpc_id      = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-ecs"
  })
}

resource "aws_vpc_security_group_ingress_rule" "ecs_from_alb" {
  for_each = local.application_ports

  security_group_id            = aws_security_group.ecs.id
  description                  = "Trafic ${each.key} depuis l'ALB"
  referenced_security_group_id = aws_security_group.alb.id
  from_port                    = each.value
  to_port                      = each.value
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "ecs_jakarta_from_frontend" {
  security_group_id            = aws_security_group.ecs.id
  description                  = "Jakarta depuis le frontend ECS"
  referenced_security_group_id = aws_security_group.ecs.id
  from_port                    = local.application_ports.jakarta
  to_port                      = local.application_ports.jakarta
  ip_protocol                  = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "ecs_outbound" {
  security_group_id = aws_security_group.ecs.id
  description       = "Sortie via le NAT Gateway"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds"
  description = "PostgreSQL uniquement depuis ECS"
  vpc_id      = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-rds"
  })
}

resource "aws_vpc_security_group_ingress_rule" "rds_postgres" {
  security_group_id            = aws_security_group.rds.id
  description                  = "PostgreSQL depuis ECS"
  referenced_security_group_id = aws_security_group.ecs.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}
