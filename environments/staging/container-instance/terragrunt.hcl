# Staging Container Instance
# Docker container service (pulled from DockerHub)

include "root" {
  path = find_in_parent_folders()
}

include "env" {
  path   = find_in_parent_folders("env.hcl")
  expose = true
}

terraform {
  source = "${get_repo_root()}/modules/container-instance"
}

dependency "resource_group" {
  config_path = "../resource-group"

  mock_outputs = {
    resource_group_name     = "mock-rg"
    resource_group_location = "eastus"
  }
}

dependency "log_analytics" {
  config_path = "../log-analytics"

  mock_outputs = {
    workspace_customer_id = "mock-workspace-id"
    primary_shared_key    = "mock-key"
  }
}

inputs = {
  container_group_name = "aci-${include.env.inputs.environment}-gogs-app"
  container_name       = "gogs-app"
  location             = dependency.resource_group.outputs.resource_group_location
  resource_group_name  = dependency.resource_group.outputs.resource_group_name
  
  # Docker image configuration - pulled from DockerHub
  docker_image       = get_env("TF_VAR_docker_image", "nginx:latest")
  dockerhub_username = get_env("TF_VAR_dockerhub_username", "")
  dockerhub_password = get_env("TF_VAR_dockerhub_password", "")
  
  # Container settings
  cpu            = 1
  memory         = 1.5
  container_port = 80
  ip_address_type = "Public"
  dns_name_label = "gogs-stg-app-${get_env("TF_VAR_unique_suffix", "001")}"
  restart_policy = "Always"
  
  # Environment variables
  environment_variables = {
    ENVIRONMENT = "staging"
  }
  
  # Secure environment variables (from Key Vault in real scenario)
  secure_environment_variables = {}
  
  # Log Analytics for diagnostics
  log_analytics_workspace_id  = dependency.log_analytics.outputs.workspace_customer_id
  log_analytics_workspace_key = dependency.log_analytics.outputs.primary_shared_key
  
  tags = include.env.inputs.tags
}
