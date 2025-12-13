# Staging Environment Configuration
# Include the root terragrunt.hcl

include "root" {
  path = find_in_parent_folders()
}

# Environment-specific inputs
inputs = {
  environment = "staging"
  location    = "eastus"
  
  tags = {
    Environment = "staging"
    Project     = "gogs-infra"
    ManagedBy   = "Terragrunt"
  }
}
