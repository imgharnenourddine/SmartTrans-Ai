variable "aws_region" {
  description = "Région AWS du bucket de state."
  type        = string
  default     = "eu-west-3"
}

variable "state_bucket_name" {
  description = "Nom du bucket S3 destiné au state Terraform."
  type        = string
  default     = "smarttrans-ai-terraform-state-409171460717"
}

variable "project" {
  description = "Tag Project."
  type        = string
  default     = "SmartTrans-AI"
}

variable "environment" {
  description = "Tag Environment."
  type        = string
  default     = "bootstrap"
}

variable "managed_by" {
  description = "Tag ManagedBy."
  type        = string
  default     = "Terraform"
}
