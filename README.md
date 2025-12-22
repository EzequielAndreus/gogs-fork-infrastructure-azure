# Gogs Infrastructure - Azure

[![Infrastructure CI](https://github.com/EzequielAndreus/gogs-fork-infrastructure-azure/actions/workflows/ci.yml/badge.svg)](https://github.com/EzequielAndreus/gogs-fork-infrastructure-azure/actions/workflows/ci.yml)

This repository contains Infrastructure as Code (IaC) for deploying and managing Azure infrastructure using **Terraform** and **Terragrunt**. It follows a GitOps approach with GitHub Actions for CI (validation/testing) and Jenkins for CD (infrastructure provisioning).

## 🏗️ Architecture Overview

The infrastructure provisions Azure services using Terraform modules managed by Terragrunt:

| Service | Azure Resource | Purpose |
|---------|---------------|---------|
| **Container Service** | Azure Container Instance | Runs Gogs application from DockerHub |
| **Database** | Azure SQL Database | Managed relational database for application data |
| **Compute** | Azure Virtual Machine | Splunk monitoring and log aggregation server |
| **Secrets Management** | Azure Key Vault | Secure storage for credentials and sensitive data |
| **Networking** | VNet, Subnets, NSG | Network isolation and security rules |
| **Logging** | Log Analytics Workspace | Centralized logging and diagnostics |

### Integrations

| Tool | Purpose |
|------|---------|
| **Discord** | Real-time pipeline notifications (success/failure/abort) |
| **Jira** | Automatic ticket creation on pipeline failures |

## 📁 Repository Structure

```
gogs-fork-infrastructure-azure/
├── 📄 README.md                          # This file - project documentation
├── 📄 JENKINS-CREDENTIALS.md             # Required Jenkins credentials 
├── 📄 GH-CREDENTIALS.md                  # Required GitHub Secrets credentials 
├── 📄 Jenkinsfile                        # Main Jenkins CD pipeline
├── 📄 terragrunt.hcl                     # Root Terragrunt configuration
├── 📄 .tflint.hcl                        # TFLint configuration
├── 📄 .checkov.yml                       # Checkov security scanner config
├── 📄 build.gradle                       # Gradle build configuration
├── 📄 settings.gradle                    # Gradle settings
│
├── 📁 .github/
│   └── 📁 workflows/
│       ├── 📄 ci.yml                     # GitHub Actions CI workflow
│       └── 📄 linter.yaml                # Super-linter workflow
│
├── 📁 jenkins/
│   └── 📁 shared/           
│       └── 📄 pipeline-helpers.groovy    # Shared pipeline utility functions
│
├── 📁 test/                              # Test files
│   ├── 📁 jenkins/                       # Jenkins pipeline tests
│   │   ├── 📄 JenkinsfileTest.groovy
│   │   ├── 📄 PipelineHelpersTest.groovy
│   │   └── 📄 README.md
│   └── 📁 unit/                          # Go unit tests
│       ├── 📄 container_instance_test.go
│       ├── 📄 key_vault_test.go
│       ├── 📄 log_analytics_test.go
│       ├── 📄 networking_test.go
│       ├── 📄 resource_group_test.go
│       ├── 📄 sql_database_test.go
│       ├── 📄 virtual_machine_test.go
│       ├── 📄 go.mod
│       └── 📄 README.md
│
├── 📁 modules/                           # Reusable Terraform modules
│   ├── 📁 resource-group/                # Azure Resource Group
│   │   ├── 📄 main.tf
│   │   ├── 📄 variables.tf
│   │   └── 📄 outputs.tf
│   │
│   ├── 📁 key-vault/                     # Azure Key Vault (Secrets Manager)
│   │   ├── 📄 main.tf
│   │   ├── 📄 variables.tf
│   │   └── 📄 outputs.tf
│   │
│   ├── 📁 networking/                    # VNet, Subnets, NSGs
│   │   ├── 📄 main.tf
│   │   ├── 📄 variables.tf
│   │   └── 📄 outputs.tf
│   │
│   ├── 📁 container-instance/            # Azure Container Instance
│   │   ├── 📄 main.tf
│   │   ├── 📄 variables.tf
│   │   └── 📄 outputs.tf
│   │
│   ├── 📁 sql-database/                  # Azure SQL Database (RDS)
│   │   ├── 📄 main.tf
│   │   ├── 📄 variables.tf
│   │   └── 📄 outputs.tf
│   │
│   ├── 📁 virtual-machine/               # Azure VM (EC2) for Splunk
│   │   ├── 📄 main.tf
│   │   ├── 📄 variables.tf
│   │   └── 📄 outputs.tf
│   │
│   └── 📁 log-analytics/                 # Log Analytics Workspace
│       ├── 📄 main.tf
│       ├── 📄 variables.tf
│       └── 📄 outputs.tf
│
└── 📁 environments/                      # Environment-specific configurations
    ├── 📁 staging/
    │   ├── 📄 env.hcl                    # Staging environment variables
    │   ├── 📁 resource-group/
    │   │   └── 📄 terragrunt.hcl
    │   ├── 📁 key-vault/
    │   │   └── 📄 terragrunt.hcl
    │   ├── 📁 networking/
    │   │   └── 📄 terragrunt.hcl
    │   ├── 📁 log-analytics/
    │   │   └── 📄 terragrunt.hcl
    │   ├── 📁 container-instance/
    │   │   └── 📄 terragrunt.hcl
    │   ├── 📁 sql-database/
    │   │   └── 📄 terragrunt.hcl
    │   └── 📁 splunk-vm/
    │       └── 📄 terragrunt.hcl
    │
    └── 📁 production/
        ├── 📄 env.hcl                    # Production environment variables
        ├── 📁 resource-group/
        │   └── 📄 terragrunt.hcl
        ├── 📁 key-vault/
        │   └── 📄 terragrunt.hcl
        ├── 📁 networking/
        │   └── 📄 terragrunt.hcl
        ├── 📁 log-analytics/
        │   └── 📄 terragrunt.hcl
        ├── 📁 container-instance/
        │   └── 📄 terragrunt.hcl
        ├── 📁 sql-database/
        │   └── 📄 terragrunt.hcl
        └── 📁 splunk-vm/
            └── 📄 terragrunt.hcl
```

## 📝 File Importance

### Root Configuration Files

| File | Importance | Description |
|------|------------|-------------|
| `terragrunt.hcl` | 🔴 Critical | Root Terragrunt configuration with remote state and provider setup |
| `Jenkinsfile` | 🔴 Critical | Main Jenkins pipeline for CD operations |
| `.tflint.hcl` | 🟡 Important | TFLint rules for code quality |
| `.checkov.yml` | 🟡 Important | Checkov security scanner configuration |
| `.github/workflows/ci.yml` | 🔴 Critical | GitHub Actions CI pipeline |
| `.github/workflows/linter.yaml` | 🟡 Important | Super-linter for code quality (Groovy, Markdown, Terraform, Terragrunt) |

### Terraform Modules

| Module | Importance | Description |
|--------|------------|-------------|
| `resource-group` | 🔴 Critical | Base resource - all other resources depend on it |
| `key-vault` | 🔴 Critical | Stores all sensitive credentials securely |
| `networking` | 🔴 Critical | Network isolation and security rules |
| `container-instance` | 🔴 Critical | Runs the main application container |
| `sql-database` | 🔴 Critical | Application database |
| `virtual-machine` | 🟡 Important | Splunk monitoring server |
| `log-analytics` | 🟡 Important | Centralized logging and diagnostics |

### Environment Configurations

| Environment | Purpose |
|-------------|---------|
| `staging` | Pre-production testing environment |
| `production` | Live production environment |

## 🔄 CI/CD Pipeline

### CI Pipeline (GitHub Actions)

The CI pipeline runs on every PR and push to `main`:

1. **Format Check** - Validates Terraform formatting
2. **Validate** - Validates Terraform syntax for all 7 modules
3. **TFLint** - Lints Terraform code for best practices
4. **Security Scan (Checkov)** - Scans for security misconfigurations
5. **Security Scan (tfsec)** - Additional security scanning  
6. **Documentation Check** - Verifies README files and checks for TODOs
7. **Cost Estimation** - Estimates infrastructure costs with Infracost (PR only, optional)

**Note:** Terragrunt plan/apply are intentionally excluded from CI for performance and security. These run in the CD pipeline with proper Azure credentials and approval gates.

### CD Pipeline (Jenkins)

The CD pipeline handles infrastructure provisioning with automatic change detection:

1. **Initialize** - Loads shared utilities, sets up environment
2. **Setup Tools** - Installs/verifies Terraform and Terragrunt
3. **Azure Login** - Authenticates with service principal
4. **Plan Staging** - Generates plan, detects if changes exist
5. **Apply Staging** - Auto-applies if changes detected
6. **Plan Production** - Generates plan, detects if changes exist  
7. **Approval** - Manual approval required for production changes
8. **Apply Production** - Applies changes after approval

**Automatic Change Detection:** Pipeline only applies when Terragrunt detects actual infrastructure changes, skipping unnecessary applies.

### 📢 Notifications & Alerting

| Event | Discord | Jira |
|-------|---------|------|
| ✅ Success | ✓ Green notification | - |
| ❌ Failure | ✓ Red notification | ✓ Bug ticket created |
| ⚠️ Aborted | ✓ Yellow notification | - |

**Discord**: Real-time notifications for all pipeline events
**Jira**: Automatic ticket creation on failures (Medium priority for staging, Highest for production)

## 🚀 Getting Started

### Prerequisites

- [Terraform](https://www.terraform.io/downloads) >= 1.5.7
- [Terragrunt](https://terragrunt.gruntwork.io/docs/getting-started/install/) >= 0.53.0  
- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
- Azure subscription with Contributor permissions
- Azure Storage Account for Terraform state (configured in terragrunt.hcl)

### Local Development

1. **Clone the repository:**
   ```bash
   git clone https://github.com/EzequielAndreus/gogs-fork-infrastructure-azure.git
   cd gogs-fork-infrastructure-azure
   ```

2. **Configure Azure credentials:**
   ```bash
   az login
   az account set --subscription <subscription-id>
   ```

3. **Set required environment variables:**
   ```bash
   export TF_VAR_db_admin_username="sqladmin"
   export TF_VAR_db_admin_password="<secure-password>"
   export TF_VAR_dockerhub_username="<dockerhub-user>"
   export TF_VAR_dockerhub_password="<dockerhub-token>"
   export TF_VAR_splunk_ssh_public_key="$(cat ~/.ssh/id_rsa.pub)"
   export TF_VAR_unique_suffix="001"
   ```

4. **Initialize and plan (staging):**
   ```bash
   cd environments/staging
   terragrunt run-all init
   terragrunt run-all plan
   ```

5. **Apply changes:**
   ```bash
   terragrunt run-all apply
   ```

### Deploying a Specific Module

```bash
cd environments/staging/container-instance
terragrunt apply
```

## 🌍 Environments

### Staging
- Lower resource specifications
- Shorter backup retention
- Auto-pause enabled for databases
- Less restrictive network rules

### Production
- Higher resource specifications
- Longer backup retention (35+ days)
- Zone redundancy enabled
- Purge protection on Key Vault
- Restrictive network ACLs

## 📊 Module Dependencies

```
resource-group
     │
     ├── key-vault
     │
     ├── networking
     │        │
     │        ├── container-instance
     │        │
     │        ├── sql-database
     │        │
     │        └── splunk-vm
     │
     └── log-analytics
              │
              └── container-instance
```

## 🔒 Security Considerations

1. **Secrets Management**: All sensitive data stored in Azure Key Vault
2. **Network Isolation**: Resources deployed in private subnets with NSGs
3. **TLS Enforcement**: Minimum TLS 1.2 for all services
4. **RBAC**: Use Azure AD for access control
5. **Audit Logging**: Log Analytics for comprehensive logging

## 🧪 Testing

### Run Format Check
```bash
terraform fmt -check -recursive
```

### Run TFLint
```bash
tflint --init
tflint --chdir=modules/
```

### Run Security Scan
```bash
checkov -d modules/ --framework terraform
```

## 📚 Additional Resources

- [Terraform Azure Provider Documentation](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)
- [Terragrunt Documentation](https://terragrunt.gruntwork.io/docs/)
- [Azure Architecture Center](https://docs.microsoft.com/en-us/azure/architecture/)

## 📄 License

This project is licensed under the MIT License.

## 👥 Contributing

1. Create a feature branch from `main`
2. Make your changes
3. Ensure CI passes
4. Submit a pull request
5. Get approval and merge

---

**Note**: See [JENKINS-CREDENTIALS.md](./JENKINS-CREDENTIALS.md) for Jenkins CD credential setup and [GH-CREDENTIALS.md](./GH-CREDENTIALS.md) for GitHub Actions CI credential setup.