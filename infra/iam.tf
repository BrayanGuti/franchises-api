# IAM de mínimo privilegio: tres roles, cada uno con solo lo que necesita.

# ---- Rol de ejecución: ECS lo usa para descargar la imagen y escribir logs ---------------------

resource "aws_iam_role" "execution" {
  name               = "${var.project}-execution"
  assume_role_policy = data.aws_iam_policy_document.assume_ecs_tasks.json
}

data "aws_iam_policy_document" "execution" {
  # GetAuthorizationToken no admite restringir el recurso.
  statement {
    sid       = "EcrLogin"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid       = "PullImage"
    actions   = ["ecr:BatchCheckLayerAvailability", "ecr:GetDownloadUrlForLayer", "ecr:BatchGetImage"]
    resources = [aws_ecr_repository.this.arn]
  }

  statement {
    sid       = "WriteLogs"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.this.arn}:*"]
  }
}

resource "aws_iam_role_policy" "execution" {
  name   = "pull-image-and-write-logs"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.execution.json
}

# ---- Rol de la tarea: lo usa la aplicación -----------------------------------------------------

resource "aws_iam_role" "task" {
  name               = "${var.project}-task"
  assume_role_policy = data.aws_iam_policy_document.assume_ecs_tasks.json
}

data "aws_iam_policy_document" "task" {
  # Exactamente las operaciones que usa la aplicación, solo sobre su tabla. Las transacciones
  # (TransactWriteItems) se autorizan con los permisos de cada operación que contienen.
  # Sin CreateTable, DeleteTable ni Scan.
  statement {
    sid = "ApplicationTable"
    actions = [
      "dynamodb:GetItem",
      "dynamodb:PutItem",
      "dynamodb:UpdateItem",
      "dynamodb:DeleteItem",
      "dynamodb:Query",
      "dynamodb:ConditionCheckItem",
    ]
    resources = [aws_dynamodb_table.this.arn]
  }
}

resource "aws_iam_role_policy" "task" {
  name   = "application-table"
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.task.json
}

# ---- Rol de infraestructura: ECS Express lo usa para administrar balanceador, red y certificado --

resource "aws_iam_role" "infrastructure" {
  name               = "${var.project}-infrastructure"
  assume_role_policy = data.aws_iam_policy_document.assume_ecs.json
}

# Política administrada por AWS, específica de Express Mode.
resource "aws_iam_role_policy_attachment" "infrastructure" {
  role       = aws_iam_role.infrastructure.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSInfrastructureRoleforExpressGatewayServices"
}

# ---- Quién puede asumir cada rol ---------------------------------------------------------------

data "aws_iam_policy_document" "assume_ecs_tasks" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "assume_ecs" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs.amazonaws.com"]
    }
  }
}
