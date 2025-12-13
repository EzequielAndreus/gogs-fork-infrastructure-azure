# Azure SQL Database Module
# AWS RDS equivalent for managed relational database

resource "azurerm_mssql_server" "main" {
  name                         = var.sql_server_name
  resource_group_name          = var.resource_group_name
  location                     = var.location
  version                      = var.sql_version
  administrator_login          = var.admin_username
  administrator_login_password = var.admin_password
  minimum_tls_version          = var.minimum_tls_version

  azuread_administrator {
    login_username = var.azuread_admin_username
    object_id      = var.azuread_admin_object_id
  }

  tags = var.tags
}

resource "azurerm_mssql_database" "main" {
  name                        = var.database_name
  server_id                   = azurerm_mssql_server.main.id
  collation                   = var.collation
  max_size_gb                 = var.max_size_gb
  sku_name                    = var.sku_name
  zone_redundant              = var.zone_redundant
  auto_pause_delay_in_minutes = var.auto_pause_delay_in_minutes
  min_capacity                = var.min_capacity

  short_term_retention_policy {
    retention_days           = var.backup_retention_days
    backup_interval_in_hours = var.backup_interval_hours
  }

  long_term_retention_policy {
    weekly_retention  = var.ltr_weekly_retention
    monthly_retention = var.ltr_monthly_retention
    yearly_retention  = var.ltr_yearly_retention
    week_of_year      = var.ltr_week_of_year
  }

  tags = var.tags
}

# Virtual Network Rule for private access
resource "azurerm_mssql_virtual_network_rule" "main" {
  count     = var.subnet_id != "" ? 1 : 0
  name      = "${var.sql_server_name}-vnet-rule"
  server_id = azurerm_mssql_server.main.id
  subnet_id = var.subnet_id
}

# Firewall rule for Azure services
resource "azurerm_mssql_firewall_rule" "allow_azure_services" {
  count            = var.allow_azure_services ? 1 : 0
  name             = "AllowAzureServices"
  server_id        = azurerm_mssql_server.main.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}

# Custom firewall rules
resource "azurerm_mssql_firewall_rule" "custom" {
  for_each         = var.firewall_rules
  name             = each.key
  server_id        = azurerm_mssql_server.main.id
  start_ip_address = each.value.start_ip
  end_ip_address   = each.value.end_ip
}
