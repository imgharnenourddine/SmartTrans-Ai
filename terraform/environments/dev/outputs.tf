output "aws_region" {
  description = "Région AWS de l'environnement dev."
  value       = var.aws_region
}

output "environment" {
  description = "Nom de l'environnement."
  value       = var.environment
}

output "vpc_id" {
  description = "Identifiant du VPC dev."
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Sous-réseaux publics."
  value       = { for key, subnet in aws_subnet.public : key => subnet.id }
}

output "private_subnet_ids" {
  description = "Sous-réseaux privés."
  value       = { for key, subnet in aws_subnet.private : key => subnet.id }
}

output "alb_security_group_id" {
  description = "Groupe de sécurité de l'ALB."
  value       = aws_security_group.alb.id
}

output "ecs_security_group_id" {
  description = "Groupe de sécurité ECS."
  value       = aws_security_group.ecs.id
}

output "rds_security_group_id" {
  description = "Groupe de sécurité RDS."
  value       = aws_security_group.rds.id
}

output "ecr_repository_urls" {
  description = "URLs des dépôts ECR privés."
  value       = { for name, repo in aws_ecr_repository.app : name => repo.repository_url }
}

output "alb_dns_name" {
  description = "Nom DNS public de l'ALB."
  value       = aws_lb.main.dns_name
}

output "ecs_cluster_name" {
  description = "Nom du cluster ECS."
  value       = aws_ecs_cluster.main.name
}

output "rds_endpoint" {
  description = "Hôte PostgreSQL privé. Le mot de passe n'est pas exposé."
  value       = aws_db_instance.main.address
}

output "postgresql_secret_arn" {
  description = "ARN du secret PostgreSQL. La valeur n'est pas exposée."
  value       = aws_secretsmanager_secret.db.arn
}

output "jwt_secret_arn" {
  description = "ARN du secret JWT. La valeur n'est pas exposée."
  value       = aws_secretsmanager_secret.jwt.arn
}
