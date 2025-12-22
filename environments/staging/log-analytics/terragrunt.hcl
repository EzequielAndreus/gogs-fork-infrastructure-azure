# Staging Log Analytics
# Centralized logging and monitoring

include "root" {
  path = find_in_parent_folders()
}

include "env" {
  path   = find_in_parent_folders("env.hcl")
  expose = true
}

terraform {
  source = "${get_repo_root()}/modules/log-analytics"
}

dependency "resource_group" {
  config_path = "../resource-group"

  mock_outputs = {
    resource_group_name     = "mock-rg"
    resource_group_location = "eastus"
  }
}

inputs = {
  workspace_name            = "law-${include.env.inputs.environment}-gogs-infra"
  location                  = dependency.resource_group.outputs.resource_group_location
  resource_group_name       = dependency.resource_group.outputs.resource_group_name
  sku                       = "PerGB2018"
  retention_in_days         = 30
  enable_container_insights = true
  enable_sql_analytics      = true
  
  tags = include.env.inputs.tags
}
