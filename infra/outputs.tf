locals {
  # ECS Express entrega el dominio del servicio (servicio.ecs.region.on.aws), con HTTPS incluido.
  endpoint = try(aws_ecs_express_gateway_service.api.ingress_paths[0].endpoint, "")
  api_url  = startswith(local.endpoint, "http") ? local.endpoint : "https://${local.endpoint}"
}

output "api_url" {
  description = "URL pública de la API (HTTPS)."
  value       = local.api_url
}

output "swagger_url" {
  description = "Documentación de la API."
  value       = "${trimsuffix(local.api_url, "/")}/swagger-ui.html"
}

output "ecr_repository_url" {
  description = "Repositorio donde se sube la imagen."
  value       = aws_ecr_repository.this.repository_url
}

output "dynamodb_table_name" {
  description = "Tabla de DynamoDB de la aplicación."
  value       = aws_dynamodb_table.this.name
}

output "ecs_service_arn" {
  description = "Servicio de ECS Express."
  value       = aws_ecs_express_gateway_service.api.service_arn
}
