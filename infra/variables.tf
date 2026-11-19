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

variable "image_tag" {
  description = "Tag de la imagen a desplegar. El CD pasa el SHA del commit."
  type        = string

  validation {
    condition     = length(var.image_tag) > 0
    error_message = "image_tag no puede estar vacío."
  }
}
