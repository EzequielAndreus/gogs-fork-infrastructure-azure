variable "sql_server_name" {
  description = "Name of the SQL server"
  type        = string
}

variable "database_name" {
  description = "Name of the database"
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

variable "sql_version" {
  description = "SQL Server version"
  type        = string
  default     = "12.0"
}

variable "admin_username" {
  description = "Administrator username"
  type        = string
  sensitive   = true
}

variable "admin_password" {
  description = "Administrator password"
  type        = string
  sensitive   = true
}

variable "minimum_tls_version" {
  description = "Minimum TLS version"
  type        = string
  default     = "1.2"
}

variable "azuread_admin_username" {
  description = "Azure AD administrator username"
  type        = string
  default     = ""
}

variable "azuread_admin_object_id" {
  description = "Azure AD administrator object ID"
  type        = string
  default     = ""
}

variable "collation" {
  description = "Database collation"
  type        = string
  default     = "SQL_Latin1_General_CP1_CI_AS"
}

variable "max_size_gb" {
  description = "Maximum size of the database in GB"
  type        = number
  default     = 32
}

variable "sku_name" {
  description = "SKU name for the database"
  type        = string
  default     = "GP_S_Gen5_2"
}

variable "zone_redundant" {
  description = "Enable zone redundancy"
  type        = bool
  default     = false
}

variable "auto_pause_delay_in_minutes" {
  description = "Auto-pause delay in minutes (-1 to disable)"
  type        = number
  default     = 60
}

variable "min_capacity" {
  description = "Minimum capacity for serverless"
  type        = number
  default     = 0.5
}

variable "backup_retention_days" {
  description = "Short-term backup retention days"
  type        = number
  default     = 7
}

variable "backup_interval_hours" {
  description = "Backup interval in hours"
  type        = number
  default     = 12
}

variable "ltr_weekly_retention" {
  description = "Long-term retention - weekly"
  type        = string
  default     = "P1W"
}

variable "ltr_monthly_retention" {
  description = "Long-term retention - monthly"
  type        = string
  default     = "P1M"
}

variable "ltr_yearly_retention" {
  description = "Long-term retention - yearly"
  type        = string
  default     = "P1Y"
}

variable "ltr_week_of_year" {
  description = "Week of year for yearly backup"
  type        = number
  default     = 1
}

variable "subnet_id" {
  description = "Subnet ID for VNet integration"
  type        = string
  default     = ""
}

variable "allow_azure_services" {
  description = "Allow Azure services to access the database"
  type        = bool
  default     = true
}

variable "firewall_rules" {
  description = "Map of firewall rules"
  type = map(object({
    start_ip = string
    end_ip   = string
  }))
  default = {}
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
