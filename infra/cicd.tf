# Acceso de GitHub Actions a AWS con OIDC: sin llaves guardadas. En cada ejecución GitHub firma un
# token que AWS acepta solo si viene de este repositorio, y entrega credenciales temporales.
# - Rol "plan": solo lectura, para mostrar el plan de Terraform en los pull requests.
# - Rol "deploy": solo desde la rama main; sube la imagen y aplica, limitado a este proyecto.
#
# Ningún rol puede modificar los roles del pipeline ni el proveedor OIDC: eso solo se cambia con un
# apply local, para que un workflow no pueda ampliar sus propios permisos.

data "aws_caller_identity" "current" {}

locals {
  account_id   = data.aws_caller_identity.current.account_id
  state_bucket = "${var.project}-tfstate-${local.account_id}"
  oidc_subject = var.github_oidc_subject_prefix
  app_roles = [
    aws_iam_role.execution.arn,
    aws_iam_role.task.arn,
    aws_iam_role.infrastructure.arn,
  ]
}

resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

# ---- Rol plan: pull requests y main, solo lectura ----------------------------------------------

resource "aws_iam_role" "github_plan" {
  name               = "${var.project}-github-plan"
  assume_role_policy = data.aws_iam_policy_document.assume_github_plan.json
}

data "aws_iam_policy_document" "assume_github_plan" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["${local.oidc_subject}:pull_request", "${local.oidc_subject}:ref:refs/heads/main"]
    }
  }
}

# Lo que Terraform necesita para leer el estado y comparar con AWS. Sin acceso a datos de la tabla.
data "aws_iam_policy_document" "read_infrastructure" {
  statement {
    sid = "DescribeResources"
    actions = [
      "dynamodb:DescribeTable", "dynamodb:DescribeContinuousBackups", "dynamodb:DescribeTimeToLive",
      "dynamodb:ListTagsOfResource",
      "ecr:DescribeRepositories", "ecr:DescribeImages", "ecr:GetLifecyclePolicy", "ecr:ListTagsForResource",
      "ecs:Describe*", "ecs:List*",
      "logs:DescribeLogGroups", "logs:ListTagsForResource",
      "iam:Get*", "iam:List*",
    ]
    resources = ["*"]
  }

  statement {
    sid       = "ReadState"
    actions   = ["s3:ListBucket", "s3:GetObject"]
    resources = ["arn:aws:s3:::${local.state_bucket}", "arn:aws:s3:::${local.state_bucket}/*"]
  }
}

resource "aws_iam_role_policy" "github_plan" {
  name   = "read-infrastructure"
  role   = aws_iam_role.github_plan.id
  policy = data.aws_iam_policy_document.read_infrastructure.json
}

# ---- Rol deploy: solo la rama main --------------------------------------------------------------

resource "aws_iam_role" "github_deploy" {
  name               = "${var.project}-github-deploy"
  assume_role_policy = data.aws_iam_policy_document.assume_github_deploy.json
}

data "aws_iam_policy_document" "assume_github_deploy" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    # El job de despliegue usa el environment "production", así que GitHub firma el token con
    # "environment:production" en lugar de la rama. La restricción a main la aplica el environment
    # (Deployment branches: solo main).
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["${local.oidc_subject}:environment:production"]
    }
  }
}

data "aws_iam_policy_document" "deploy" {
  statement {
    sid       = "EcrLogin"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid       = "ManageRepositoryAndPushImages"
    actions   = ["ecr:*"]
    resources = [aws_ecr_repository.this.arn]
  }

  # Gestión de la tabla, sin acceso a sus datos.
  statement {
    sid = "ManageTable"
    actions = [
      "dynamodb:UpdateTable", "dynamodb:UpdateContinuousBackups",
      "dynamodb:TagResource", "dynamodb:UntagResource",
    ]
    resources = [aws_dynamodb_table.this.arn]
  }

  statement {
    sid       = "ManageLogs"
    actions   = ["logs:*"]
    resources = [aws_cloudwatch_log_group.this.arn, "${aws_cloudwatch_log_group.this.arn}:*"]
  }

  # ECS Express no documenta permisos por recurso para todas sus acciones; la cuenta solo aloja
  # este proyecto. Queda anotado como compromiso aceptado.
  statement {
    sid       = "ManageService"
    actions   = ["ecs:*"]
    resources = ["*"]
  }

  # Solo los tres roles de la aplicación; nunca los roles del pipeline.
  statement {
    sid = "ManageApplicationRoles"
    actions = [
      "iam:UpdateRole", "iam:UpdateAssumeRolePolicy", "iam:TagRole", "iam:UntagRole",
      "iam:PutRolePolicy", "iam:DeleteRolePolicy", "iam:AttachRolePolicy", "iam:DetachRolePolicy",
      "iam:PassRole",
    ]
    resources = local.app_roles
  }

  statement {
    sid       = "WriteStateAndLock"
    actions   = ["s3:PutObject", "s3:DeleteObject"]
    resources = ["arn:aws:s3:::${local.state_bucket}/*"]
  }
}

resource "aws_iam_role_policy" "github_deploy_read" {
  name   = "read-infrastructure"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.read_infrastructure.json
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "deploy"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.deploy.json
}
