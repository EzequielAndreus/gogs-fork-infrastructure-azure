# Azure Resource Group Module
# Creates a resource group to contain all infrastructure resources

resource "azurerm_resource_group" "main" {
  name     = var.resource_group_name
  location = var.location

  tags = var.tags
}
