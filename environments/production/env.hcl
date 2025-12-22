# Production Environment Configuration
# Include the root terragrunt.hcl

include "root" {
  path = find_in_parent_folders()
}

# Environment-specific inputs
inputs = {
  environment = "production"
  location    = "eastus"
  
  tags = {
    Environment = "production"
    Project     = "gogs-infra"
    ManagedBy   = "Terragrunt"
  }
}
