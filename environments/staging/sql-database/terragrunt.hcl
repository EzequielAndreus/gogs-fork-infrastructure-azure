# Staging SQL Database
# Azure SQL Database (AWS RDS equivalent)

include "root" {
  path = find_in_parent_folders()
}

include "env" {
  path   = find_in_parent_folders("env.hcl")
  expose = true
}

terraform {
  source = "${get_repo_root()}/modules/sql-database"
}

dependency "resource_group" {
  config_path = "../resource-group"

  mock_outputs = {
    resource_group_name     = "mock-rg"
    resource_group_location = "eastus"
  }
}

dependency "networking" {
  config_path = "../networking"

  mock_outputs = {
    database_subnet_id = "mock-subnet-id"
  }
}

inputs = {
  sql_server_name = "sql-stg-gogs-${get_env("TF_VAR_unique_suffix", "001")}"
  database_name   = "gogsdb"
  location        = dependency.resource_group.outputs.resource_group_location
  resource_group_name = dependency.resource_group.outputs.resource_group_name
  
  # Credentials (from environment variables)
  admin_username = get_env("TF_VAR_db_admin_username", "sqladmin")
  admin_password = get_env("TF_VAR_db_admin_password", "")
  
  # Azure AD Admin (optional)
  azuread_admin_username  = get_env("TF_VAR_azuread_admin_username", "")
  azuread_admin_object_id = get_env("TF_VAR_azuread_admin_object_id", "")
  
  # Database configuration
  sql_version                 = "12.0"
  minimum_tls_version         = "1.2"
  sku_name                    = "GP_S_Gen5_2"  # Serverless General Purpose
  max_size_gb                 = 32
  zone_redundant              = false
  auto_pause_delay_in_minutes = 60
  min_capacity                = 0.5
  
  # Backup configuration
  backup_retention_days = 7
  backup_interval_hours = 12
  ltr_weekly_retention  = "P1W"
  ltr_monthly_retention = "P1M"
  ltr_yearly_retention  = "P1Y"
  ltr_week_of_year      = 1
  
  # Network configuration
  subnet_id            = dependency.networking.outputs.database_subnet_id
  allow_azure_services = true
  
  firewall_rules = {
    # Add allowed IPs here
  }
  
  tags = include.env.inputs.tags
}
