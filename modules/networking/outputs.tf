output "vnet_id" {
  description = "ID of the virtual network"
  value       = azurerm_virtual_network.main.id
}

output "vnet_name" {
  description = "Name of the virtual network"
  value       = azurerm_virtual_network.main.name
}

output "container_subnet_id" {
  description = "ID of the container subnet"
  value       = azurerm_subnet.container.id
}

output "database_subnet_id" {
  description = "ID of the database subnet"
  value       = azurerm_subnet.database.id
}

output "vm_subnet_id" {
  description = "ID of the VM subnet"
  value       = azurerm_subnet.vm.id
}

output "container_nsg_id" {
  description = "ID of the container NSG"
  value       = azurerm_network_security_group.container.id
}

output "vm_nsg_id" {
  description = "ID of the VM NSG"
  value       = azurerm_network_security_group.vm.id
}
