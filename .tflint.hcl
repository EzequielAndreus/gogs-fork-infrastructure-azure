# TFLint Configuration
# https://github.com/terraform-linters/tflint

config {
  module = true
}

plugin "azurerm" {
  enabled = true
  version = "0.25.1"
  source  = "github.com/terraform-linters/tflint-ruleset-azurerm"
}

# Enforce naming conventions
rule "terraform_naming_convention" {
  enabled = true
}

# Enforce required providers
rule "terraform_required_providers" {
  enabled = true
}

# Enforce required version
rule "terraform_required_version" {
  enabled = true
}

# Enforce standard module structure
rule "terraform_standard_module_structure" {
  enabled = true
}

# Warn about unused declarations
rule "terraform_unused_declarations" {
  enabled = true
}

# Warn about deprecated syntax
rule "terraform_deprecated_interpolation" {
  enabled = true
}

# Azure-specific rules
rule "azurerm_resource_missing_tags" {
  enabled = true
}
