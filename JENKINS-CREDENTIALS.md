# 🔐 Jenkins CD Credentials Guide

This guide covers all credentials required for the **Jenkins CD pipeline** (`Jenkinsfile`). The Jenkins pipeline deploys infrastructure changes to staging and production environments with approval gates.

---

## 📋 Quick Reference

| Credential | Purpose | Environment |
|------------|---------|-------------|
| Azure Service Principal | Infrastructure provisioning | Both |
| Terraform State Config | State backend access | Both |
| Staging Application Vars | Staging deployment | Staging |
| Production Application Vars | Production deployment | Production |
| Discord Webhook | Pipeline notifications | Both |
| Jira Integration | Failure ticket creation | Both |

---

## 🔑 Azure Service Principal

Jenkins requires Azure credentials with **full Contributor access** to provision and manage infrastructure.

### Creating the Service Principal

```bash
# Create Service Principal for CD (deployment)
az ad sp create-for-rbac \
  --name "gogs-infrastructure-cd" \
  --role "Contributor" \
  --scopes /subscriptions/<SUBSCRIPTION_ID>
```

Save the output values for Jenkins credential configuration.

### Required Azure Permissions

| Role | Scope | Purpose |
|------|-------|---------|
| **Contributor** | Subscription | Create/modify/delete resources |
| **Storage Blob Data Contributor** | State Storage Account | Manage Terraform state |
| **Key Vault Administrator** | Key Vaults | Manage secrets during deployment |

### Granting Additional Permissions

```bash
# Key Vault Administrator role
az role assignment create \
  --assignee <SERVICE_PRINCIPAL_CLIENT_ID> \
  --role "Key Vault Administrator" \
  --scope /subscriptions/<SUBSCRIPTION_ID>
```

### Verifying the Service Principal

```bash
# Test authentication
az login --service-principal \
  -u <CLIENT_ID> \
  -p <CLIENT_SECRET> \
  --tenant <TENANT_ID>

# Verify permissions
az role assignment list --assignee <CLIENT_ID>
```

---

## 📦 Terraform State Backend

Jenkins uses the same state backend as CI but with write permissions.

### Required State Storage Configuration

```bash
RESOURCE_GROUP="tfstate-rg"
STORAGE_ACCOUNT="tfstategogs001"  # Must be globally unique
CONTAINER="tfstate"
LOCATION="eastus"
```

### Granting Write Access

```bash
# Get storage account ID
STORAGE_ID=$(az storage account show \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --query id -o tsv)

# Grant write access to CD Service Principal
az role assignment create \
  --assignee <CD_SERVICE_PRINCIPAL_CLIENT_ID> \
  --role "Storage Blob Data Contributor" \
  --scope $STORAGE_ID
```

---

## 🎯 Environment-Specific Credentials

Jenkins uses **different credentials for staging and production** to ensure proper isolation and security.

### Staging Environment Variables

| Credential | Jenkins ID | Description | Example |
|------------|------------|-------------|---------|
| Unique Suffix | `tf-unique-suffix-staging` | Resource naming suffix | `stg` |
| DB Admin Username | `db-admin-username-staging` | SQL admin username | `sqladmin` |
| DB Admin Password | `db-admin-password-staging` | SQL admin password | `SecureStaging123!` |
| DockerHub Username | `dockerhub-username` | DockerHub username | `myuser` |
| DockerHub Password | `dockerhub-password` | DockerHub access token | `dckr_pat_xxx` |
| SSH Public Key | `splunk-ssh-public-key-staging` | Splunk VM SSH key | `ssh-rsa AAA...` |
| Docker Image | `docker-image-staging` | Container image | `myapp:staging` |

### Production Environment Variables

| Credential | Jenkins ID | Description | Example |
|------------|------------|-------------|---------|
| Unique Suffix | `tf-unique-suffix-prod` | Resource naming suffix | `prod` |
| DB Admin Username | `db-admin-username-prod` | SQL admin username | `sqladmin` |
| DB Admin Password | `db-admin-password-prod` | SQL admin password | `SecureProd456!` |
| SSH Public Key | `splunk-ssh-public-key-prod` | Splunk VM SSH key | `ssh-rsa BBB...` |
| Docker Image | `docker-image-production` | Container image | `myapp:v1.2.3` |
| Admin IP Range | `admin-ip-range-prod` | Admin access CIDR | `203.0.113.0/24` |

**Note:** DockerHub credentials are shared between environments but can be separated if needed.

---

## 🔧 Jenkins Credential Configuration

### Adding Credentials to Jenkins

1. Navigate to **Jenkins Dashboard** → **Manage Jenkins** → **Credentials**
2. Select domain: **(global)**
3. Click **Add Credentials**

### Credential Types

| Type | When to Use |
|------|-------------|
| **Secret text** | API keys, tokens, passwords, IDs |
| **Secret file** | SSH keys, certificates |
| **Username with password** | DockerHub, service accounts |

### Azure Credentials

**Type:** Secret text (for each)

```yaml
ARM_CLIENT_ID:
  ID: arm-client-id
  Description: Azure Service Principal Client ID
  Secret: <CLIENT_ID>

ARM_CLIENT_SECRET:
  ID: arm-client-secret
  Description: Azure Service Principal Secret
  Secret: <CLIENT_SECRET>

ARM_SUBSCRIPTION_ID:
  ID: arm-subscription-id
  Description: Azure Subscription ID
  Secret: <SUBSCRIPTION_ID>

ARM_TENANT_ID:
  ID: arm-tenant-id
  Description: Azure Tenant ID
  Secret: <TENANT_ID>
```

### Terraform State Credentials

**Type:** Secret text (for each)

```yaml
TF_STATE_RESOURCE_GROUP:
  ID: tf-state-resource-group
  Description: Terraform state resource group
  Secret: tfstate-rg

TF_STATE_STORAGE_ACCOUNT:
  ID: tf-state-storage-account
  Description: Terraform state storage account
  Secret: tfstategogs001

TF_STATE_CONTAINER:
  ID: tf-state-container
  Description: Terraform state container
  Secret: tfstate
```

### Staging Application Credentials

**Type:** Secret text (for each)

```yaml
tf-unique-suffix-staging:
  Description: Unique suffix for staging resources
  Secret: stg

db-admin-username-staging:
  Description: Staging database admin username
  Secret: sqladmin

db-admin-password-staging:
  Description: Staging database admin password
  Secret: <strong-password>

splunk-ssh-public-key-staging:
  Description: Staging Splunk VM SSH public key
  Secret: ssh-rsa AAAAB3NzaC1...

docker-image-staging:
  Description: Docker image for staging
  Secret: myorg/myapp:staging
```

### Production Application Credentials

**Type:** Secret text (for each)

```yaml
tf-unique-suffix-prod:
  Description: Unique suffix for production resources
  Secret: prod

db-admin-username-prod:
  Description: Production database admin username
  Secret: sqladmin

db-admin-password-prod:
  Description: Production database admin password
  Secret: <strong-password>

splunk-ssh-public-key-prod:
  Description: Production Splunk VM SSH public key
  Secret: ssh-rsa AAAAB3NzaC1...

docker-image-production:
  Description: Docker image for production
  Secret: myorg/myapp:v1.2.3

admin-ip-range-prod:
  Description: Admin IP range for production access
  Secret: 203.0.113.0/24
```

### Shared Application Credentials

**Type:** Username with password (DockerHub)

```yaml
dockerhub-credentials:
  ID: dockerhub-username, dockerhub-password
  Description: DockerHub authentication
  Username: <dockerhub-username>
  Password: <dockerhub-access-token>
```

---

## 📢 Discord Webhook Integration

Discord webhooks send real-time notifications for pipeline events.

### Creating a Discord Webhook

1. Open Discord and go to your server
2. Right-click on the channel → **Edit Channel**
3. Go to **Integrations** → **Webhooks**
4. Click **New Webhook**
5. Name: `Jenkins Infrastructure`
6. Copy the **Webhook URL**

### Discord Webhook Configuration

**Type:** Secret text

```yaml
DISCORD_WEBHOOK_URL:
  ID: discord-webhook-url
  Description: Discord webhook for pipeline notifications
  Secret: https://discord.com/api/webhooks/...
```

### Notification Types Sent

| Event | Staging | Production |
|-------|---------|------------|
| Pipeline Started | ✅ | ✅ |
| Apply Started | ✅ | ✅ |
| Approval Required | ❌ | ✅ |
| Success | ✅ | ✅ |
| Failure | ✅ | ✅ |
| Aborted | ✅ | ✅ |

### Testing Discord Integration

```bash
# Test webhook
curl -X POST "https://discord.com/api/webhooks/..." \
  -H "Content-Type: application/json" \
  -d '{
    "embeds": [{
      "title": "Test Notification",
      "description": "Testing Jenkins integration",
      "color": 3066993
    }]
  }'
```

---

## 🎫 Jira Integration

Jira integration automatically creates tickets when the Jenkins pipeline fails.

### Creating a Jira API Token

1. Log in to your Atlassian account
2. Go to [Security Settings](https://id.atlassian.com/manage-profile/security/api-tokens)
3. Click **Create API token**
4. Label: `Jenkins Infrastructure CD`
5. Copy the token immediately

### Jira Configuration

**Type:** Secret text (for each)

```yaml
JIRA_URL:
  ID: jira-url
  Description: Jira instance URL
  Secret: https://yourcompany.atlassian.net

JIRA_USER:
  ID: jira-user
  Description: Jira service account email
  Secret: jenkins-bot@yourcompany.com

JIRA_API_TOKEN:
  ID: jira-api-token
  Description: Jira API token
  Secret: ATATT3xF...

JIRA_PROJECT_KEY:
  ID: jira-project-key
  Description: Jira project key for infrastructure issues
  Secret: INFRA
```

### Ticket Creation Behavior

| Environment | Priority | Labels |
|-------------|----------|--------|
| Staging | Medium | `infrastructure`, `jenkins`, `automated`, `staging` |
| Production | Highest | `infrastructure`, `jenkins`, `automated`, `production`, `critical` |

### Required Jira Project Setup

Ensure your Jira project has:
- **Bug** issue type enabled
- **Priority** field with: Low, Medium, High, Highest
- **Labels** field enabled
- Service account permissions:
  - Create issues
  - Edit issues
  - Add comments

### Testing Jira Integration

```bash
# Test authentication
curl -X GET "https://yourcompany.atlassian.net/rest/api/3/myself" \
  -H "Authorization: Basic $(echo -n 'user@email.com:API_TOKEN' | base64)" \
  -H "Content-Type: application/json"

# Test issue creation
curl -X POST "https://yourcompany.atlassian.net/rest/api/3/issue" \
  -H "Authorization: Basic $(echo -n 'user@email.com:API_TOKEN' | base64)" \
  -H "Content-Type: application/json" \
  -d '{
    "fields": {
      "project": {"key": "INFRA"},
      "summary": "Test Issue",
      "description": "Test",
      "issuetype": {"name": "Bug"}
    }
  }'
```

---

## 🔄 Credential Rotation

### Recommended Rotation Schedule

| Credential | Frequency | Impact |
|------------|-----------|--------|
| Azure SP Secret | 90 days | High - Update immediately |
| Database Passwords | 90 days | Medium - Requires redeploy |
| DockerHub Token | 180 days | Low - Update during maintenance |
| SSH Keys | Annually | Medium - Requires VM access update |
| Jira API Token | As needed | Low - Only affects notifications |
| Discord Webhook | As needed | Low - Only affects notifications |

### Rotating Azure Service Principal Secret

```bash
# Generate new secret
NEW_SECRET=$(az ad sp credential reset \
  --id <CLIENT_ID> \
  --query password -o tsv)

# Update in Jenkins
# 1. Go to Manage Jenkins → Credentials
# 2. Find ARM_CLIENT_SECRET
# 3. Update secret value
# 4. Save

# Verify by triggering a pipeline run
```

### Rotating Database Passwords

```bash
# Generate new password
NEW_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-20)

# Update Jenkins credentials
# 1. Update db-admin-password-staging
# 2. Update db-admin-password-prod

# Apply changes
# Run Terragrunt apply to update database
```

### Rotating SSH Keys

```bash
# Generate new key pair
ssh-keygen -t rsa -b 4096 -C "splunk-prod-$(date +%Y%m%d)" -f ~/.ssh/splunk_prod_new

# Update Jenkins credential with new public key
# Update splunk-ssh-public-key-prod

# Deploy via Terragrunt apply
```

---

## 🔐 Security Best Practices

### DO ✅

- Use separate credentials for staging and production
- Rotate credentials on schedule
- Use service accounts (not personal accounts)
- Monitor Jenkins audit logs
- Use strong, randomly generated passwords
- Restrict Jenkins access with RBAC
- Enable Jenkins security realm
- Use separate Azure subscriptions for prod (if possible)

### DON'T ❌

- Share production credentials with staging
- Use the same Service Principal for CI and CD
- Grant Owner role to Service Principal
- Store credentials in pipeline code
- Use personal email for service accounts
- Share credentials via Slack/email
- Use weak or guessable passwords

---

## 🧪 Testing Jenkins Credentials

### Validating Azure Access

```bash
# Test Service Principal login
az login --service-principal \
  -u $ARM_CLIENT_ID \
  -p $ARM_CLIENT_SECRET \
  --tenant $ARM_TENANT_ID

# Test resource creation
az group create \
  --name test-jenkins-access \
  --location eastus

# Cleanup
az group delete --name test-jenkins-access --yes
```

### Validating State Backend Access

```bash
# List state files
az storage blob list \
  --account-name tfstategogs001 \
  --container-name tfstate \
  --auth-mode login
```

### Testing Pipeline Locally

```bash
# Set environment variables
export ARM_CLIENT_ID="..."
export ARM_CLIENT_SECRET="..."
export ARM_SUBSCRIPTION_ID="..."
export ARM_TENANT_ID="..."
export TF_STATE_RESOURCE_GROUP="tfstate-rg"
export TF_STATE_STORAGE_ACCOUNT="tfstategogs001"
export TF_STATE_CONTAINER="tfstate"

# Navigate to environment
cd environments/staging

# Test plan
terragrunt run-all plan
```

---

## 📝 Jenkins Credential Checklist

Before running the Jenkins CD pipeline:

### Azure Setup
- [ ] CD Service Principal created with Contributor role
- [ ] Service Principal has Storage Blob Data Contributor on state account
- [ ] Service Principal has Key Vault Administrator role
- [ ] Azure credentials added to Jenkins (4 secrets)
- [ ] Terraform state configuration added (3 secrets)

### Staging Setup
- [ ] Staging unique suffix configured
- [ ] Staging database credentials configured
- [ ] Staging SSH public key added
- [ ] Staging Docker image configured
- [ ] DockerHub credentials added (if private registry)

### Production Setup
- [ ] Production unique suffix configured
- [ ] Production database credentials configured
- [ ] Production SSH public key added
- [ ] Production Docker image configured
- [ ] Production admin IP range configured

### Integration Setup
- [ ] Discord webhook created and URL added
- [ ] Jira API token created
- [ ] Jira project configured for issues
- [ ] Jira credentials added to Jenkins (4 secrets)

### Verification
- [ ] All credentials added to Jenkins
- [ ] Service Principal permissions verified
- [ ] Test pipeline run successful
- [ ] Discord notifications working
- [ ] Jira ticket creation tested (optional)

---

## 🆘 Troubleshooting

### "Error: Insufficient permissions to perform action"

**Cause:** Service Principal lacks required Azure role.

**Solution:**
```bash
# Check current role assignments
az role assignment list --assignee <CLIENT_ID>

# Add Contributor role
az role assignment create \
  --assignee <CLIENT_ID> \
  --role "Contributor" \
  --scope /subscriptions/<SUBSCRIPTION_ID>
```

### "Error: Failed to save state"

**Cause:** No write access to state storage account.

**Solution:**
```bash
# Grant Storage Blob Data Contributor role
az role assignment create \
  --assignee <CLIENT_ID> \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<SUB_ID>/resourceGroups/tfstate-rg/providers/Microsoft.Storage/storageAccounts/tfstategogs001
```

### "Error: Key Vault access denied"

**Cause:** Service Principal lacks Key Vault permissions.

**Solution:**
```bash
# Grant Key Vault Administrator role
az role assignment create \
  --assignee <CLIENT_ID> \
  --role "Key Vault Administrator" \
  --scope /subscriptions/<SUBSCRIPTION_ID>
```

### Discord Notifications Not Sent

**Cause:** Invalid webhook URL or webhook deleted.

**Solution:**
1. Verify webhook exists in Discord channel settings
2. Test webhook with curl command
3. Update `DISCORD_WEBHOOK_URL` in Jenkins
4. Check Jenkins console output for HTTP errors

### Jira Ticket Not Created

**Cause:** Invalid API token or incorrect project configuration.

**Solution:**
1. Test Jira authentication with curl
2. Verify project key exists
3. Check service account has create issue permission
4. Review Jenkins console for Jira API errors

---

## 📚 Related Documentation

- [Jenkinsfile](Jenkinsfile) - CD pipeline definition
- [GH-CREDENTIALS.md](GH-CREDENTIALS.md) - CI pipeline credentials
- [jenkins/shared/InfraUtils.groovy](jenkins/shared/InfraUtils.groovy) - Pipeline helper functions
- [MODULES.md](MODULES.md) - Infrastructure modules
- [README.md](README.md) - Project overview

---

## 📞 Support

For Jenkins credential issues:

1. Check Jenkins console output for specific errors
2. Verify all credentials are configured in **Manage Jenkins** → **Credentials**
3. Test Azure Service Principal locally using Azure CLI
4. Review Azure portal for role assignments
5. Check Discord/Jira integrations separately

**Important:** The Jenkins pipeline deploys to production - ensure all credentials are properly secured and tested in staging first.
