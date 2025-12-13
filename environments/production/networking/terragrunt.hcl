# Production Networking
# Virtual Network, Subnets, and Network Security Groups

include "root" {
  path = find_in_parent_folders()
}

include "env" {
  path   = find_in_parent_folders("env.hcl")
  expose = true
}

terraform {
  source = "${get_repo_root()}/modules/networking"
}

dependency "resource_group" {
  config_path = "../resource-group"

  mock_outputs = {
    resource_group_name     = "mock-rg"
    resource_group_location = "eastus"
  }
}

inputs = {
  vnet_name               = "vnet-${include.env.inputs.environment}-gogs-infra"
  location                = dependency.resource_group.outputs.resource_group_location
  resource_group_name     = dependency.resource_group.outputs.resource_group_name
  address_space           = ["10.1.0.0/16"]  # Different from staging
  container_subnet_prefix = "10.1.1.0/24"
  database_subnet_prefix  = "10.1.2.0/24"
  vm_subnet_prefix        = "10.1.3.0/24"
  admin_ip_range          = get_env("TF_VAR_admin_ip_range", "0.0.0.0/0")  # Should be restricted
  
  tags = include.env.inputs.tags
}
