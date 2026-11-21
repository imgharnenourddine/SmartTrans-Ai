terraform {
  backend "s3" {
    bucket       = "smarttrans-ai-terraform-state-409171460717"
    key          = "dev/terraform.tfstate"
    region       = "eu-west-3"
    use_lockfile = true
  }
}
