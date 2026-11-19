variable "aws_region" {
  description = "Región de AWS."
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Nombre del proyecto; prefijo de los recursos."
  type        = string
  default     = "franchises-api"
}

variable "table_name" {
  description = "Nombre de la tabla de DynamoDB."
  type        = string
  default     = "franchises"
}

variable "github_oidc_subject_prefix" {
  description = <<-EOT
    Prefijo del claim "sub" de los tokens OIDC de GitHub para este repositorio: solo ese repositorio
    puede usar los roles del pipeline. GitHub lo firma en formato inmutable, con los IDs numéricos del
    propietario y del repositorio (repo:propietario@id/nombre@id), así que un repositorio borrado y
    vuelto a crear con el mismo nombre no hereda el acceso. Se obtiene con:
      gh api repos/<propietario>/<repositorio>/actions/oidc/customization/sub --jq .sub_claim_prefix
  EOT
  type        = string
  default     = "repo:BrayanGuti@99362850/franchises-api@1463144534"

  validation {
    condition     = startswith(var.github_oidc_subject_prefix, "repo:")
    error_message = "El prefijo debe empezar por \"repo:\"."
  }
}

variable "image_tag" {
  description = "Tag de la imagen a desplegar. El CD pasa el SHA del commit."
  type        = string

  validation {
    condition     = length(var.image_tag) > 0
    error_message = "image_tag no puede estar vacío."
  }
}
