terraform {
  # 1.10+: bloqueo del estado nativo en S3 (use_lockfile), sin tabla de DynamoDB para locks.
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.71.0"
    }
  }

  # Estado remoto en S3, para que el CD (GitHub Actions) y una máquina local compartan el mismo
  # estado. Configuración parcial: el bucket va en backend.hcl (no versionado) o en -backend-config.
  backend "s3" {
    key          = "franchises-api/terraform.tfstate"
    encrypt      = true
    use_lockfile = true
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = var.project
      ManagedBy = "terraform"
    }
  }
}
