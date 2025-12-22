# Production Key Vault
# Azure Key Vault for storing sensitive credentials

include "root" {
  path = find_in_parent_folders()
}

include "env" {
  path   = find_in_parent_folders("env.hcl")
  expose = true
}

terraform {
  source = "${get_repo_root()}/modules/key-vault"
}

dependency "resource_group" {
  config_path = "../resource-group"

  mock_outputs = {
    resource_group_name     = "mock-rg"
    resource_group_location = "eastus"
  }
}

inputs = {
  key_vault_name              = "kv-prd-gogs-${get_env("TF_VAR_unique_suffix", "001")}"
  location                    = dependency.resource_group.outputs.resource_group_location
  resource_group_name         = dependency.resource_group.outputs.resource_group_name
  soft_delete_retention_days  = 90
  purge_protection_enabled    = true  # Enabled for production
  sku_name                    = "standard"
  network_acls_default_action = "Deny"  # More restrictive for production
  allowed_ip_ranges           = []  # Add allowed IPs
  
  # Database credentials
  store_db_credentials = true
  db_admin_username    = get_env("TF_VAR_db_admin_username", "sqladmin")
  db_admin_password    = get_env("TF_VAR_db_admin_password", "")
  
  # DockerHub credentials
  store_dockerhub_credentials = true
  dockerhub_username          = get_env("TF_VAR_dockerhub_username", "")
  dockerhub_password          = get_env("TF_VAR_dockerhub_password", "")
  
  tags = include.env.inputs.tags
}
