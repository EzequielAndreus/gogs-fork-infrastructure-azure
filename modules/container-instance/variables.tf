variable "container_group_name" {
  description = "Name of the container group"
  type        = string
}

variable "container_name" {
  description = "Name of the container"
  type        = string
}

variable "location" {
  description = "Azure region for resources"
  type        = string
}

variable "resource_group_name" {
  description = "Name of the resource group"
  type        = string
}

variable "docker_image" {
  description = "Docker image to deploy (e.g., 'nginx:latest' or 'myorg/myapp:v1.0')"
  type        = string
}

variable "dockerhub_username" {
  description = "DockerHub username for private images"
  type        = string
  default     = ""
  sensitive   = true
}

variable "dockerhub_password" {
  description = "DockerHub password/token for private images"
  type        = string
  default     = ""
  sensitive   = true
}

variable "cpu" {
  description = "CPU cores for the container"
  type        = number
  default     = 1
}

variable "memory" {
  description = "Memory in GB for the container"
  type        = number
  default     = 1.5
}

variable "container_port" {
  description = "Port exposed by the container"
  type        = number
  default     = 80
}

variable "ip_address_type" {
  description = "IP address type (Public or Private)"
  type        = string
  default     = "Public"
}

variable "dns_name_label" {
  description = "DNS name label for the container group"
  type        = string
  default     = null
}

variable "os_type" {
  description = "Operating system type"
  type        = string
  default     = "Linux"
}

variable "restart_policy" {
  description = "Restart policy (Always, OnFailure, Never)"
  type        = string
  default     = "Always"
}

variable "environment_variables" {
  description = "Environment variables for the container"
  type        = map(string)
  default     = {}
}

variable "secure_environment_variables" {
  description = "Secure environment variables for the container"
  type        = map(string)
  default     = {}
  sensitive   = true
}

variable "volumes" {
  description = "Volumes to mount in the container"
  type = list(object({
    name                 = string
    mount_path           = string
    read_only            = optional(bool)
    empty_dir            = optional(bool)
    storage_account_name = optional(string)
    storage_account_key  = optional(string)
    share_name           = optional(string)
  }))
  default = []
}

variable "log_analytics_workspace_id" {
  description = "Log Analytics workspace ID for diagnostics"
  type        = string
  default     = ""
}

variable "log_analytics_workspace_key" {
  description = "Log Analytics workspace key for diagnostics"
  type        = string
  default     = ""
  sensitive   = true
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
