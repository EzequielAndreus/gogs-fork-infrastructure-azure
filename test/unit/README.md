# Terraform Module Tests

This directory contains unit tests for all Terraform modules using [Terratest](https://terratest.gruntwork.io/).

## Prerequisites

- Go 1.21+
- Terraform 1.5.7+
- Azure CLI with authenticated session
- Azure subscription with contributor access

## Setup

```bash
# Install Go dependencies
cd test
go mod download

# Login to Azure
az login
az account set --subscription "YOUR_SUBSCRIPTION_ID"
```

## Running Tests

### Run All Tests

```bash
cd test
go test -v -timeout 60m ./...
```

### Run Specific Module Test

```bash
# Test resource-group module
go test -v -timeout 30m -run TestResourceGroupModule

# Test networking module
go test -v -timeout 30m -run TestNetworkingModule

# Test key-vault module
go test -v -timeout 30m -run TestKeyVaultModule

# Test container-instance module
go test -v -timeout 30m -run TestContainerInstanceModule

# Test sql-database module
go test -v -timeout 45m -run TestSqlDatabaseModule

# Test virtual-machine module
go test -v -timeout 45m -run TestVirtualMachineModule

# Test log-analytics module
go test -v -timeout 30m -run TestLogAnalyticsModule
```

## Test Structure

```text
test/
├── README.md
├── go.mod
├── go.sum
├── resource_group_test.go
├── key_vault_test.go
├── networking_test.go
├── container_instance_test.go
├── sql_database_test.go
├── virtual_machine_test.go
└── log_analytics_test.go
```

## Environment Variables

| Variable | Description | Required |
| -------- | ----------- | -------- |
| `ARM_SUBSCRIPTION_ID` | Azure Subscription ID | Yes |
| `ARM_CLIENT_ID` | Service Principal Client ID | For CI/CD |
| `ARM_CLIENT_SECRET` | Service Principal Secret | For CI/CD |
| `ARM_TENANT_ID` | Azure Tenant ID | For CI/CD |

## Notes

- Tests create real Azure resources and destroy them after completion
- Each test uses a unique random suffix to avoid naming conflicts
- Tests are designed to be idempotent and isolated
- Timeout is set to handle Azure resource provisioning times

>Additional note: Given the need of managing the infra state, Terraform Cloud might be integrated into the URL.