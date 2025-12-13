variable "vnet_name" {
  description = "Name of the virtual network"
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

variable "address_space" {
  description = "Address space for the virtual network"
  type        = list(string)
  default     = ["10.0.0.0/16"]
}

variable "container_subnet_prefix" {
  description = "Address prefix for container subnet"
  type        = string
  default     = "10.0.1.0/24"
}

variable "database_subnet_prefix" {
  description = "Address prefix for database subnet"
  type        = string
  default     = "10.0.2.0/24"
}

variable "vm_subnet_prefix" {
  description = "Address prefix for VM subnet"
  type        = string
  default     = "10.0.3.0/24"
}

variable "admin_ip_range" {
  description = "IP range allowed to access admin resources"
  type        = string
  default     = "*"
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
