# Terraform Modules

This document provides an overview of all Terraform modules available in this infrastructure repository.

## Module Overview

| Module | Description | Azure Service Equivalent |
| ------ | ----------- | ------------------------ |
| [resource-group](#resource-group) | Resource container for all infrastructure | Azure Resource Group |
| [key-vault](#key-vault) | Secure credential storage | AWS Secrets Manager equivalent |
| [networking](#networking) | Virtual network infrastructure | VPC equivalent |
| [container-instance](#container-instance) | Docker container hosting | ECS/Fargate equivalent |
| [sql-database](#sql-database) | Managed SQL database | RDS equivalent |
| [virtual-machine](#virtual-machine) | Linux VM for Splunk monitoring | EC2 equivalent |
| [log-analytics](#log-analytics) | Centralized logging and diagnostics | CloudWatch equivalent |

---

## Module Details

### resource-group

**Path:** `modules/resource-group`

**Description:**  
Creates an Azure Resource Group that serves as a logical container for all infrastructure resources. All other modules depend on this resource group.

**Key Resources:**
- `azurerm_resource_group` - The resource group container

**Inputs:**
- `resource_group_name` - Name of the resource group
- `location` - Azure region for deployment
- `tags` - Resource tags for organization

**Outputs:**
- `name` - Resource group name
- `location` - Resource group location
- `id` - Resource group ID

---

### key-vault

**Path:** `modules/key-vault`

**Description:**  
Provisions an Azure Key Vault for secure storage of sensitive credentials including database passwords, Docker Hub credentials, and other secrets. Equivalent to AWS Secrets Manager.

**Key Resources:**
- `azurerm_key_vault` - Key Vault instance with access policies
- `azurerm_key_vault_secret` - Stored secrets (database, DockerHub credentials)

**Features:**
- Soft delete protection
- Network ACLs for access control
- Access policies for service principals
- Automatic storage of database and container registry credentials

**Inputs:**
- `key_vault_name` - Name of the Key Vault
- `sku_name` - SKU tier (standard/premium)
- `store_db_credentials` - Whether to store database credentials
- `store_dockerhub_credentials` - Whether to store DockerHub credentials

**Outputs:**
- `id` - Key Vault ID
- `vault_uri` - Key Vault URI
- `name` - Key Vault name

---

### networking

**Path:** `modules/networking`

**Description:**  
Creates the virtual network infrastructure including VNet, subnets, and Network Security Groups (NSGs). Provides network isolation and security for all infrastructure components.

**Key Resources:**
- `azurerm_virtual_network` - Main virtual network
- `azurerm_subnet` - Three subnets (container, database, VM)
- `azurerm_network_security_group` - NSGs for container and VM subnets

**Subnet Layout:**

| Subnet | Purpose | Features |
| ------ | ------- | -------- |
| container-subnet | Container Instances | Container delegation enabled |
| database-subnet | Azure SQL Database | SQL service endpoint enabled |
| vm-subnet | Virtual Machines (Splunk) | Standard subnet |

**Security Rules:**
- Container NSG: Allows HTTP (80) and HTTPS (443)
- VM NSG: Allows SSH (22), Splunk Web (8000), Splunk Forwarder (9997) from admin IPs

**Inputs:**
- `vnet_name` - Virtual network name
- `address_space` - CIDR block for VNet
- `container_subnet_prefix` - CIDR for container subnet
- `database_subnet_prefix` - CIDR for database subnet
- `vm_subnet_prefix` - CIDR for VM subnet
- `admin_ip_range` - Allowed admin IP range for SSH/Splunk access

**Outputs:**
- `vnet_id` - Virtual network ID
- `container_subnet_id` - Container subnet ID
- `database_subnet_id` - Database subnet ID
- `vm_subnet_id` - VM subnet ID
- `vm_nsg_id` - VM Network Security Group ID

---

### container-instance

**Path:** `modules/container-instance`

**Description:**  
Deploys Azure Container Instances to run Docker containers pulled from DockerHub. Provides a serverless container hosting solution similar to AWS ECS Fargate.

**Key Resources:**
- `azurerm_container_group` - Container group with Docker image

**Features:**
- DockerHub private registry authentication
- Environment variables (standard and secure)
- Volume mounts support
- Log Analytics integration for diagnostics
- Configurable CPU and memory allocation

**Inputs:**
- `container_group_name` - Container group name
- `docker_image` - Docker image to deploy (e.g., `nginx:latest`)
- `container_port` - Port exposed by container
- `cpu` - CPU cores allocated
- `memory` - Memory in GB allocated
- `dockerhub_username/password` - DockerHub credentials for private images
- `environment_variables` - Non-sensitive environment variables
- `secure_environment_variables` - Sensitive environment variables
- `log_analytics_workspace_id/key` - Log Analytics for diagnostics

**Outputs:**
- `id` - Container group ID
- `fqdn` - Fully qualified domain name
- `ip_address` - Container public IP address

---

### sql-database

**Path:** `modules/sql-database`

**Description:**  
Provisions an Azure SQL Database with a managed SQL Server. Equivalent to AWS RDS, provides a fully managed relational database service.

**Key Resources:**
- `azurerm_mssql_server` - SQL Server instance
- `azurerm_mssql_database` - Database instance
- `azurerm_mssql_virtual_network_rule` - VNet integration
- `azurerm_mssql_firewall_rule` - Firewall rules

**Features:**
- Azure AD authentication support
- VNet integration for private access
- Configurable firewall rules
- Short-term and long-term backup retention
- Zone redundancy option
- Auto-pause for serverless tier

**Inputs:**
- `sql_server_name` - SQL Server name
- `database_name` - Database name
- `admin_username/password` - SQL admin credentials
- `sku_name` - Database tier/size
- `max_size_gb` - Maximum database size
- `subnet_id` - Subnet for VNet integration
- `backup_retention_days` - Short-term backup retention

**Outputs:**
- `server_id` - SQL Server ID
- `server_fqdn` - SQL Server fully qualified domain name
- `database_id` - Database ID
- `database_name` - Database name
- `connection_string` - ADO.NET connection string

---

### virtual-machine

**Path:** `modules/virtual-machine`

**Description:**  
Deploys an Azure Linux Virtual Machine configured for Splunk monitoring. Equivalent to AWS EC2, provides infrastructure for running custom workloads.

**Key Resources:**
- `azurerm_linux_virtual_machine` - Linux VM
- `azurerm_network_interface` - NIC with private/public IP
- `azurerm_public_ip` - Static public IP (optional)
- `azurerm_managed_disk` - Data disk for Splunk data
- `azurerm_virtual_machine_data_disk_attachment` - Disk attachment

**Features:**
- SSH key authentication
- System-assigned managed identity
- Custom data (cloud-init) support
- Separate data disk for Splunk data
- NSG association for security

**Inputs:**
- `vm_name` - Virtual machine name
- `vm_size` - VM size (e.g., `Standard_D2s_v3`)
- `admin_username` - Admin username
- `ssh_public_key` - SSH public key for authentication
- `subnet_id` - Subnet for VM placement
- `create_public_ip` - Whether to create public IP
- `create_data_disk` - Whether to create Splunk data disk
- `custom_data` - Cloud-init script for provisioning

**Outputs:**
- `vm_id` - Virtual machine ID
- `private_ip` - Private IP address
- `public_ip` - Public IP address (if created)
- `identity_principal_id` - Managed identity principal ID

---

### log-analytics

**Path:** `modules/log-analytics`

**Description:**  
Creates an Azure Log Analytics Workspace for centralized logging, monitoring, and diagnostics across all infrastructure components.

**Key Resources:**
- `azurerm_log_analytics_workspace` - Log Analytics workspace
- `azurerm_log_analytics_solution` - Container Insights solution
- `azurerm_log_analytics_solution` - SQL Analytics solution

**Features:**
- Configurable log retention period
- Container Insights for container monitoring
- SQL Advanced Threat Protection
- Centralized log aggregation

**Inputs:**
- `workspace_name` - Workspace name
- `sku` - Pricing tier (PerGB2018)
- `retention_in_days` - Log retention period
- `enable_container_insights` - Enable container monitoring
- `enable_sql_analytics` - Enable SQL monitoring

**Outputs:**
- `id` - Workspace ID
- `workspace_id` - Workspace GUID (for agents)
- `primary_shared_key` - Primary shared key (for agents)
- `name` - Workspace name

---

## Module Dependencies

```text
resource-group
     │
     ├── log-analytics
     │
     ├── key-vault
     │
     ├── networking
     │        │
     │        ├── container-instance
     │        │
     │        ├── sql-database
     │        │
     │        └── virtual-machine
     │
     └── (all other modules depend on resource-group)
```

## Usage Example

```hcl
module "resource_group" {
  source = "./modules/resource-group"
  
  resource_group_name = "my-infrastructure-rg"
  location            = "eastus"
  tags                = { Environment = "staging" }
}

module "networking" {
  source = "./modules/networking"
  
  resource_group_name = module.resource_group.name
  location            = module.resource_group.location
  vnet_name           = "my-vnet"
  address_space       = ["10.0.0.0/16"]
  # ... additional configuration
}
```
> Terraform Cloud is the proposed backend to store states. Any further information will be updated ASAP. Expected credential to be added: Terraform Cloud API Token.
