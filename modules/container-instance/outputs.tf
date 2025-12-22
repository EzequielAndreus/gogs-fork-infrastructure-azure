output "container_group_id" {
  description = "ID of the container group"
  value       = azurerm_container_group.main.id
}

output "container_group_name" {
  description = "Name of the container group"
  value       = azurerm_container_group.main.name
}

output "container_fqdn" {
  description = "FQDN of the container group"
  value       = azurerm_container_group.main.fqdn
}

output "container_ip_address" {
  description = "IP address of the container group"
  value       = azurerm_container_group.main.ip_address
}
