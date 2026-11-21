variable "aws_region" {
  description = "Région AWS de l'environnement dev."
  type        = string
  default     = "eu-west-3"
}

variable "environment" {
  description = "Nom de l'environnement."
  type        = string
  default     = "dev"
}
