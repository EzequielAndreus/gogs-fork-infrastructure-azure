# 🔐 GitHub Actions CI Credentials Guide

This guide covers all credentials required for the **GitHub Actions CI pipeline** (`.github/workflows/ci.yml`). The CI pipeline performs validation, linting, security scanning, and cost estimation for infrastructure changes.

---

## 📋 Quick Reference

| Credential | Purpose | Used In |
|------------|---------|---------|
| Azure Service Principal | Terraform validation & plan | All jobs requiring Azure auth |
| Terraform State Config | State backend access | Terragrunt plan jobs |
| Application Variables | Infrastructure validation | Terragrunt plan jobs |
| Infracost API Key | Cost estimation | Cost estimate job |

---

## 🔑 Azure Service Principal

The CI pipeline requires Azure credentials for **validation and planning only** (no apply operations).

### Creating the Service Principal

```bash
# Create Service Principal for CI/CD
az ad sp create-for-rbac \
  --name "gogs-infrastructure-ci" \
  --role "Contributor" \
  --scopes /subscriptions/<SUBSCRIPTION_ID> \
  --sdk-auth
```

Save the JSON output - you'll need these values for GitHub secrets.

### Required Azure Permissions

| Role | Scope | Purpose |
|------|-------|---------|
| **Contributor** | Subscription | Read resources for validation |
| **Storage Blob Data Contributor** | State Storage Account | Access Terraform state |

**Note:** The CI pipeline only reads and validates - it never applies changes.

### Verifying the Service Principal

```bash
# Test login
az login --service-principal \
  -u <CLIENT_ID> \
  -p <CLIENT_SECRET> \
  --tenant <TENANT_ID>

# Verify access
az account show
```

---

## 📦 Terraform State Backend

The CI pipeline needs access to the Terraform state for planning operations.

### Creating the State Storage Account

```bash
# Set variables
RESOURCE_GROUP="tfstate-rg"
STORAGE_ACCOUNT="tfstategogs001"  # Must be globally unique
CONTAINER="tfstate"
LOCATION="eastus"

# Create resource group
az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION

# Create storage account
az storage account create \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --sku Standard_LRS \
  --encryption-services blob

# Create blob container
az storage container create \
  --name $CONTAINER \
  --account-name $STORAGE_ACCOUNT
```

### Granting Service Principal Access

```bash
# Get storage account ID
STORAGE_ID=$(az storage account show \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --query id -o tsv)

# Assign role to Service Principal
az role assignment create \
  --assignee <SERVICE_PRINCIPAL_CLIENT_ID> \
  --role "Storage Blob Data Contributor" \
  --scope $STORAGE_ID
```

---

## 🔧 Application Variables

These variables are required for the CI pipeline to perform infrastructure validation and planning.

### Required Variables

| Variable | Description | Example | Used In |
|----------|-------------|---------|---------|
| `TF_VAR_unique_suffix` | Unique suffix for resources | `001` | Terragrunt plan |
| `TF_VAR_db_admin_username` | SQL admin username | `sqladmin` | Terragrunt plan |
| `TF_VAR_db_admin_password` | SQL admin password | `SecureP@ss123!` | Terragrunt plan |
| `TF_VAR_dockerhub_username` | DockerHub username | `myuser` | Terragrunt plan |
| `TF_VAR_dockerhub_password` | DockerHub access token | `dckr_pat_xxx` | Terragrunt plan |
| `TF_VAR_splunk_ssh_public_key` | SSH public key for Splunk | `ssh-rsa AAA...` | Terragrunt plan |

### Generating Secure Passwords

```bash
# Generate strong database password
openssl rand -base64 24 | tr -d "=+/" | cut -c1-20
```

### Creating DockerHub Access Token

1. Log in to [DockerHub](https://hub.docker.com/)
2. Go to **Account Settings** → **Security**
3. Click **New Access Token**
4. Name: `github-actions-ci`
5. Permissions: **Read-only**
6. Copy the token (shown only once)

### Generating SSH Key for Splunk

```bash
# Generate new SSH key pair
ssh-keygen -t rsa -b 4096 -C "splunk-ci" -f ~/.ssh/splunk_ci
# Use the public key content: cat ~/.ssh/splunk_ci.pub
```

---

## 💰 Infracost API Key

Infracost provides cost estimation for infrastructure changes in pull requests.

### Setting Up Infracost

1. Sign up at [Infracost Cloud](https://dashboard.infracost.io/)
2. Create an organization
3. Go to **Org Settings** → **API Keys**
4. Click **Create API Key**
5. Name: `github-actions-ci`
6. Copy the API key

**Note:** Infracost is free for open-source projects and has a free tier for private repositories.

---

## ⚙️ GitHub Actions Secrets Configuration

Add all credentials as **repository secrets** in GitHub:

### Navigation

1. Go to your repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**

### Complete Secret List

| Secret Name | Value | Source |
|-------------|-------|--------|
| `ARM_CLIENT_ID` | Azure Service Principal Client ID | `az ad sp create-for-rbac` output |
| `ARM_CLIENT_SECRET` | Azure Service Principal Secret | `az ad sp create-for-rbac` output |
| `ARM_SUBSCRIPTION_ID` | Azure Subscription ID | `az account show --query id -o tsv` |
| `ARM_TENANT_ID` | Azure Tenant ID | `az ad sp create-for-rbac` output |
| `TF_STATE_RESOURCE_GROUP` | `tfstate-rg` | State storage resource group |
| `TF_STATE_STORAGE_ACCOUNT` | `tfstategogs001` | State storage account name |
| `TF_STATE_CONTAINER` | `tfstate` | State blob container name |
| `TF_VAR_UNIQUE_SUFFIX` | `001` | Unique suffix for resources |
| `TF_VAR_DB_ADMIN_USERNAME` | `sqladmin` | SQL admin username |
| `TF_VAR_DB_ADMIN_PASSWORD` | Generated password | Strong password (min 12 chars) |
| `TF_VAR_DOCKERHUB_USERNAME` | DockerHub username | DockerHub account |
| `TF_VAR_DOCKERHUB_PASSWORD` | DockerHub access token | Read-only access token |
| `TF_VAR_SPLUNK_SSH_PUBLIC_KEY` | SSH public key content | `cat ~/.ssh/splunk_ci.pub` |
| `INFRACOST_API_KEY` | Infracost API key | Infracost dashboard |

### Adding Secrets via CLI (Optional)

```bash
# Install GitHub CLI
# https://cli.github.com/

# Authenticate
gh auth login

# Add secrets
gh secret set ARM_CLIENT_ID -b"<value>"
gh secret set ARM_CLIENT_SECRET -b"<value>"
gh secret set ARM_SUBSCRIPTION_ID -b"<value>"
gh secret set ARM_TENANT_ID -b"<value>"
gh secret set TF_STATE_RESOURCE_GROUP -b"tfstate-rg"
gh secret set TF_STATE_STORAGE_ACCOUNT -b"tfstategogs001"
gh secret set TF_STATE_CONTAINER -b"tfstate"
gh secret set TF_VAR_UNIQUE_SUFFIX -b"001"
gh secret set TF_VAR_DB_ADMIN_USERNAME -b"sqladmin"
gh secret set TF_VAR_DB_ADMIN_PASSWORD -b"<password>"
gh secret set TF_VAR_DOCKERHUB_USERNAME -b"<username>"
gh secret set TF_VAR_DOCKERHUB_PASSWORD -b"<token>"
gh secret set TF_VAR_SPLUNK_SSH_PUBLIC_KEY -b"<public-key>"
gh secret set INFRACOST_API_KEY -b"<api-key>"
```

---

## 🔄 Credential Rotation

### Recommended Rotation Schedule

| Credential | Frequency | Notes |
|------------|-----------|-------|
| Azure SP Secret | 90 days | Automated with Azure Key Vault |
| Database Passwords | 90 days | Update GitHub secrets |
| DockerHub Token | 180 days | Regenerate in DockerHub |
| SSH Keys | Annually | Rotate or when compromised |
| Infracost API Key | As needed | Only if compromised |

### Rotating Azure Service Principal Secret

```bash
# Generate new secret (keeps existing secret active)
az ad sp credential reset \
  --id <CLIENT_ID> \
  --append

# List all secrets
az ad sp credential list --id <CLIENT_ID>

# Update GitHub secret with new value
gh secret set ARM_CLIENT_SECRET -b"<new-secret>"

# After verification, remove old secret
az ad sp credential delete \
  --id <CLIENT_ID> \
  --key-id <OLD_KEY_ID>
```

---

## 🔐 Security Best Practices

### DO ✅

- Use read-only permissions where possible (DockerHub token)
- Rotate credentials on a regular schedule
- Use strong, randomly generated passwords
- Monitor GitHub Actions audit logs
- Use separate credentials for CI and CD pipelines
- Enable MFA on your GitHub account

### DON'T ❌

- Share secrets between CI and production environments
- Use personal credentials for CI pipelines
- Commit secrets to the repository
- Use the same password for multiple services
- Grant excessive Azure permissions

---

## 🧪 Testing the CI Pipeline

### Validating Credentials

```bash
# Test Azure authentication
az login --service-principal \
  -u $ARM_CLIENT_ID \
  -p $ARM_CLIENT_SECRET \
  --tenant $ARM_TENANT_ID

# Test state backend access
az storage blob list \
  --account-name tfstategogs001 \
  --container-name tfstate \
  --auth-mode login

# Test DockerHub authentication
echo $TF_VAR_DOCKERHUB_PASSWORD | docker login \
  --username $TF_VAR_DOCKERHUB_USERNAME \
  --password-stdin
```

### Triggering the CI Pipeline

The CI pipeline runs automatically on:
- **Pull requests** - Full validation including plan
- **Pushes to main** - Validation and linting only

To manually test:
1. Create a feature branch
2. Make a change to any `.tf` or `.hcl` file
3. Open a pull request
4. CI pipeline will run automatically

---

## 📝 Credential Checklist

Before the CI pipeline can run successfully, ensure:

- [ ] Azure Service Principal created with Contributor role
- [ ] Service Principal has access to state storage account
- [ ] Terraform state storage account created
- [ ] All 14 GitHub Actions secrets added
- [ ] DockerHub access token created (read-only)
- [ ] SSH public key generated for Splunk
- [ ] Infracost API key obtained
- [ ] Secrets verified via GitHub Actions logs

---

## 🆘 Troubleshooting

### "Error: building AzureRM Client: obtain subscription"

**Cause:** Azure credentials are incorrect or Service Principal doesn't exist.

**Solution:**
```bash
# Verify Service Principal exists
az ad sp show --id $ARM_CLIENT_ID

# Check secret is valid
az login --service-principal \
  -u $ARM_CLIENT_ID \
  -p $ARM_CLIENT_SECRET \
  --tenant $ARM_TENANT_ID
```

### "Error: Failed to get existing workspaces"

**Cause:** Cannot access Terraform state backend.

**Solution:**
```bash
# Check storage account exists
az storage account show \
  --name tfstategogs001 \
  --resource-group tfstate-rg

# Verify Service Principal has access
az role assignment list \
  --assignee $ARM_CLIENT_ID \
  --scope /subscriptions/$ARM_SUBSCRIPTION_ID/resourceGroups/tfstate-rg
```

### "Error: Unauthorized to access DockerHub"

**Cause:** DockerHub credentials are invalid or missing.

**Solution:**
- Regenerate access token in DockerHub
- Update `TF_VAR_DOCKERHUB_PASSWORD` secret
- Ensure token has read permissions

### "Infracost: API key invalid"

**Cause:** Infracost API key is incorrect or expired.

**Solution:**
- Log in to [Infracost Dashboard](https://dashboard.infracost.io/)
- Generate new API key
- Update `INFRACOST_API_KEY` secret

---

## 📚 Related Documentation

- [GitHub Actions Workflow](.github/workflows/ci.yml) - CI pipeline definition
- [JENKINS-CREDENTIALS.md](JENKINS-CREDENTIALS.md) - CD pipeline credentials
- [MODULES.md](MODULES.md) - Infrastructure modules documentation
- [README.md](README.md) - Project overview

---

## 📞 Support

For CI credential issues:

1. Check GitHub Actions logs for specific error messages
2. Verify all 14 secrets are set in repository settings
3. Test Azure credentials locally using the commands above
4. Check Azure portal for Service Principal status

**Important:** The CI pipeline never modifies infrastructure - it only validates and plans. For deployment credentials, see [JENKINS-CREDENTIALS.md](JENKINS-CREDENTIALS.md).
