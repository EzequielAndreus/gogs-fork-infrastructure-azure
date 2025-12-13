# Production Resource Group

include "root" {
  path = find_in_parent_folders()
}

include "env" {
  path   = find_in_parent_folders("env.hcl")
  expose = true
}

terraform {
  source = "${get_repo_root()}/modules/resource-group"
}

inputs = {
  resource_group_name = "rg-${include.env.inputs.environment}-gogs-infra"
  location            = include.env.inputs.location
  tags                = include.env.inputs.tags
}
