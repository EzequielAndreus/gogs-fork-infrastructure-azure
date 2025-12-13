# Azure Key Vault Module
# AWS Secrets Manager equivalent for storing sensitive credentials

data "azurerm_client_config" "current" {}

resource "azurerm_key_vault" "main" {
  name                        = var.key_vault_name
  location                    = var.location
  resource_group_name         = var.resource_group_name
  enabled_for_disk_encryption = true
  tenant_id                   = data.azurerm_client_config.current.tenant_id
  soft_delete_retention_days  = var.soft_delete_retention_days
  purge_protection_enabled    = var.purge_protection_enabled
  sku_name                    = var.sku_name

  # Access policy for the current service principal
  access_policy {
    tenant_id = data.azurerm_client_config.current.tenant_id
    object_id = data.azurerm_client_config.current.object_id

    key_permissions = [
      "Get",
      "List",
      "Create",
      "Delete",
      "Update",
      "Recover",
      "Purge",
    ]

    secret_permissions = [
      "Get",
      "List",
      "Set",
      "Delete",
      "Recover",
      "Purge",
    ]

    certificate_permissions = [
      "Get",
      "List",
      "Create",
      "Delete",
      "Update",
    ]
  }

  network_acls {
    default_action             = var.network_acls_default_action
    bypass                     = "AzureServices"
    ip_rules                   = var.allowed_ip_ranges
    virtual_network_subnet_ids = var.allowed_subnet_ids
  }

  tags = var.tags
}

# Store database credentials
resource "azurerm_key_vault_secret" "db_admin_username" {
  count        = var.store_db_credentials ? 1 : 0
  name         = "db-admin-username"
  value        = var.db_admin_username
  key_vault_id = azurerm_key_vault.main.id
}

resource "azurerm_key_vault_secret" "db_admin_password" {
  count        = var.store_db_credentials ? 1 : 0
  name         = "db-admin-password"
  value        = var.db_admin_password
  key_vault_id = azurerm_key_vault.main.id
}

# Store Docker Hub credentials
resource "azurerm_key_vault_secret" "dockerhub_username" {
  count        = var.store_dockerhub_credentials ? 1 : 0
  name         = "dockerhub-username"
  value        = var.dockerhub_username
  key_vault_id = azurerm_key_vault.main.id
}

resource "azurerm_key_vault_secret" "dockerhub_password" {
  count        = var.store_dockerhub_credentials ? 1 : 0
  name         = "dockerhub-password"
  value        = var.dockerhub_password
  key_vault_id = azurerm_key_vault.main.id
}
