# Staging Networking
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
  address_space           = ["10.0.0.0/16"]
  container_subnet_prefix = "10.0.1.0/24"
  database_subnet_prefix  = "10.0.2.0/24"
  vm_subnet_prefix        = "10.0.3.0/24"
  admin_ip_range          = "*"  # Restrict this in production
  
  tags = include.env.inputs.tags
}
