output "state_bucket_name" {
  description = "Nom du bucket S3 de state Terraform."
  value       = aws_s3_bucket.terraform_state.bucket
}

output "state_bucket_arn" {
  description = "ARN du bucket S3 de state Terraform."
  value       = aws_s3_bucket.terraform_state.arn
}
