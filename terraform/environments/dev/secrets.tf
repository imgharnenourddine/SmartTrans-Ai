resource "random_password" "db" {
  length  = 32
  special = false
}

resource "random_password" "jwt" {
  length  = 48
  special = false
}

resource "aws_secretsmanager_secret" "db" {
  name                    = "${local.name_prefix}/postgresql"
  recovery_window_in_days = 0

  tags = local.common_tags
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id
  secret_string = jsonencode({
    username = "smarttrans"
    password = random_password.db.result
    dbname   = "smarttrans"
    host     = aws_db_instance.main.address
    port     = 5432
    jdbc_url = "jdbc:postgresql://${aws_db_instance.main.address}:5432/smarttrans"
  })
}

resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${local.name_prefix}/jwt"
  recovery_window_in_days = 0

  tags = local.common_tags
}

resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id     = aws_secretsmanager_secret.jwt.id
  secret_string = random_password.jwt.result
}
