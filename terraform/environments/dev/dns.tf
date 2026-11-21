locals {
  public_domain = "smarttrans-ai.app"
  origin_domain = "origin.smarttrans-ai.app"
}

data "aws_route53_zone" "public" {
  name         = local.public_domain
  private_zone = false
}

resource "aws_acm_certificate" "alb" {
  domain_name               = local.public_domain
  subject_alternative_names = [local.origin_domain]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = local.common_tags
}

resource "aws_acm_certificate" "cloudfront" {
  provider                  = aws.us_east_1
  domain_name               = local.public_domain
  subject_alternative_names = [local.origin_domain]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = local.common_tags
}

resource "aws_route53_record" "certificate_validation" {
  for_each = toset([local.public_domain, local.origin_domain])

  zone_id = data.aws_route53_zone.public.zone_id
  name = one([
    for option in aws_acm_certificate.alb.domain_validation_options : option.resource_record_name
    if option.domain_name == each.value
  ])
  type = one([
    for option in aws_acm_certificate.alb.domain_validation_options : option.resource_record_type
    if option.domain_name == each.value
  ])
  records = [one([
    for option in aws_acm_certificate.alb.domain_validation_options : option.resource_record_value
    if option.domain_name == each.value
  ])]
  ttl             = 300
  allow_overwrite = true
}

resource "aws_acm_certificate_validation" "alb" {
  certificate_arn         = aws_acm_certificate.alb.arn
  validation_record_fqdns = [for record in aws_route53_record.certificate_validation : record.fqdn]
}

resource "aws_acm_certificate_validation" "cloudfront" {
  provider                = aws.us_east_1
  certificate_arn         = aws_acm_certificate.cloudfront.arn
  validation_record_fqdns = [for record in aws_route53_record.certificate_validation : record.fqdn]
}

resource "aws_route53_record" "origin" {
  zone_id = data.aws_route53_zone.public.zone_id
  name    = local.origin_domain
  type    = "A"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "apex_a" {
  zone_id = data.aws_route53_zone.public.zone_id
  name    = local.public_domain
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.main.domain_name
    zone_id                = aws_cloudfront_distribution.main.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "apex_aaaa" {
  zone_id = data.aws_route53_zone.public.zone_id
  name    = local.public_domain
  type    = "AAAA"

  alias {
    name                   = aws_cloudfront_distribution.main.domain_name
    zone_id                = aws_cloudfront_distribution.main.hosted_zone_id
    evaluate_target_health = false
  }
}
