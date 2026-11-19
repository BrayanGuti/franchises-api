# Infraestructura de la API en AWS:
# - DynamoDB: la tabla única de la aplicación.
# - ECR: el repositorio de la imagen Docker.
# - ECS Express Mode: ejecuta la imagen en Fargate. AWS crea y administra el balanceador de carga,
#   los grupos de seguridad y el certificado HTTPS en la VPC por defecto de la cuenta, y entrega
#   una URL pública estable.

# ---- Datos -------------------------------------------------------------------------------------

resource "aws_dynamodb_table" "this" {
  name         = var.table_name
  billing_mode = "PAY_PER_REQUEST" # on-demand: se paga por petición, sin capacidad ociosa
  hash_key     = "PK"
  range_key    = "SK"

  attribute {
    name = "PK"
    type = "S"
  }

  attribute {
    name = "SK"
    type = "S"
  }

  # Permite restaurar la tabla a cualquier segundo de los últimos 35 días.
  point_in_time_recovery {
    enabled = true
  }

  server_side_encryption {
    enabled = true
  }
}

# ---- Imagen ------------------------------------------------------------------------------------

resource "aws_ecr_repository" "this" {
  name                 = var.project
  image_tag_mutability = "IMMUTABLE" # una versión publicada (el SHA del commit) nunca se sobrescribe
  force_delete         = true        # permite terraform destroy aunque haya imágenes

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "this" {
  repository = aws_ecr_repository.this.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Conservar solo las ultimas 10 imagenes"
      selection    = { tagStatus = "any", countType = "imageCountMoreThan", countNumber = 10 }
      action       = { type = "expire" }
    }]
  })
}

# ---- Servicio ----------------------------------------------------------------------------------

resource "aws_cloudwatch_log_group" "this" {
  name              = "/ecs/${var.project}"
  retention_in_days = 14
}

resource "aws_ecs_cluster" "this" {
  name = var.project
}

resource "aws_ecs_express_gateway_service" "api" {
  service_name            = var.project
  cluster                 = aws_ecs_cluster.this.name
  cpu                     = "256" # 0,25 vCPU (arquitectura X86_64 por defecto, la de la imagen)
  memory                  = "512" # 512 MB
  execution_role_arn      = aws_iam_role.execution.arn
  task_role_arn           = aws_iam_role.task.arn
  infrastructure_role_arn = aws_iam_role.infrastructure.arn

  # Readiness: una tarea recibe tráfico solo cuando la aplicación terminó de arrancar.
  health_check_path = "/actuator/health/readiness"

  primary_container {
    image          = "${aws_ecr_repository.this.repository_url}:${var.image_tag}"
    container_port = 8080

    # Perfil prod: sin endpoint local, credenciales del rol de la tarea y sin crear la tabla.
    environment {
      name  = "SPRING_PROFILES_ACTIVE"
      value = "prod"
    }
    environment {
      name  = "APP_DYNAMODB_REGION"
      value = var.aws_region
    }
    environment {
      name  = "APP_DYNAMODB_TABLE_NAME"
      value = aws_dynamodb_table.this.name
    }

    aws_logs_configuration = [{
      log_group         = aws_cloudwatch_log_group.this.name
      log_stream_prefix = "api"
    }]
  }

  # Una sola tarea: suficiente para la prueba y mantiene el costo bajo.
  scaling_target = [{
    min_task_count            = 1
    max_task_count            = 1
    auto_scaling_metric       = "AVERAGE_CPU"
    auto_scaling_target_value = 70
  }]

  # terraform apply espera a que el servicio quede estable y sano.
  wait_for_steady_state = true

  # Los permisos deben existir antes de crear el servicio y borrarse después de él: si el rol de
  # infraestructura pierde su política primero, el servicio queda trabado al eliminarse.
  depends_on = [
    aws_iam_role_policy.execution,
    aws_iam_role_policy.task,
    aws_iam_role_policy_attachment.infrastructure,
  ]
}
