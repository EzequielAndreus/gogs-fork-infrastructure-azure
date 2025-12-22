variable "key_vault_name" {
  description = "Name of the Key Vault"
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

variable "soft_delete_retention_days" {
  description = "Number of days to retain soft-deleted items"
  type        = number
  default     = 7
}

variable "purge_protection_enabled" {
  description = "Enable purge protection"
  type        = bool
  default     = false
}

variable "sku_name" {
  description = "SKU name for the Key Vault"
  type        = string
  default     = "standard"
}

variable "network_acls_default_action" {
  description = "Default action for network ACLs"
  type        = string
  default     = "Allow"
}

variable "allowed_ip_ranges" {
  description = "List of allowed IP ranges"
  type        = list(string)
  default     = []
}

variable "allowed_subnet_ids" {
  description = "List of allowed subnet IDs"
  type        = list(string)
  default     = []
}

variable "store_db_credentials" {
  description = "Whether to store database credentials in Key Vault"
  type        = bool
  default     = false
}

variable "db_admin_username" {
  description = "Database admin username to store in Key Vault"
  type        = string
  default     = ""
  sensitive   = true
}

variable "db_admin_password" {
  description = "Database admin password to store in Key Vault"
  type        = string
  default     = ""
  sensitive   = true
}

variable "store_dockerhub_credentials" {
  description = "Whether to store DockerHub credentials in Key Vault"
  type        = bool
  default     = false
}

variable "dockerhub_username" {
  description = "DockerHub username to store in Key Vault"
  type        = string
  default     = ""
  sensitive   = true
}

variable "dockerhub_password" {
  description = "DockerHub password/token to store in Key Vault"
  type        = string
  default     = ""
  sensitive   = true
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
