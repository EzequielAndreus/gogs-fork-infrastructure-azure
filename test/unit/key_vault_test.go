package test

import (
	"fmt"
	"strings"
	"testing"

	"github.com/gruntwork-io/terratest/modules/random"
	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
)

// TestKeyVaultModule tests the key-vault module
func TestKeyVaultModule(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-kv-rg-%s", uniqueID)
	keyVaultName := fmt.Sprintf("testkv%s", uniqueID)
	location := "eastus"

	// First, create a resource group
	rgOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/resource-group",
		Vars: map[string]interface{}{
			"resource_group_name": resourceGroupName,
			"location":            location,
			"tags":                map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, rgOptions)
	terraform.InitAndApply(t, rgOptions)

	// Then create the key vault
	kvOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/key-vault",
		Vars: map[string]interface{}{
			"key_vault_name":            keyVaultName,
			"resource_group_name":       resourceGroupName,
			"location":                  location,
			"sku_name":                  "standard",
			"soft_delete_retention_days": 7,
			"purge_protection_enabled":  false,
			"store_db_credentials":      false,
			"store_dockerhub_credentials": false,
			"network_acls_default_action": "Allow",
			"allowed_ip_ranges":         []string{},
			"allowed_subnet_ids":        []string{},
			"tags":                      map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, kvOptions)
	terraform.InitAndApply(t, kvOptions)

	// Validate outputs
	outputID := terraform.Output(t, kvOptions, "id")
	outputVaultURI := terraform.Output(t, kvOptions, "vault_uri")
	outputName := terraform.Output(t, kvOptions, "name")

	// Assertions
	assert.NotEmpty(t, outputID, "Key Vault ID should not be empty")
	assert.Contains(t, outputVaultURI, keyVaultName, "Vault URI should contain key vault name")
	assert.Equal(t, keyVaultName, outputName, "Key Vault name should match")
}

// TestKeyVaultModuleWithSecrets tests key vault with stored secrets
func TestKeyVaultModuleWithSecrets(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-kv-sec-rg-%s", uniqueID)
	keyVaultName := fmt.Sprintf("testkvsec%s", uniqueID)
	location := "eastus"

	// Create resource group
	rgOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/resource-group",
		Vars: map[string]interface{}{
			"resource_group_name": resourceGroupName,
			"location":            location,
			"tags":                map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, rgOptions)
	terraform.InitAndApply(t, rgOptions)

	// Create key vault with secrets
	kvOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/key-vault",
		Vars: map[string]interface{}{
			"key_vault_name":              keyVaultName,
			"resource_group_name":         resourceGroupName,
			"location":                    location,
			"sku_name":                    "standard",
			"soft_delete_retention_days":  7,
			"purge_protection_enabled":    false,
			"store_db_credentials":        true,
			"db_admin_username":           "testadmin",
			"db_admin_password":           "TestP@ssw0rd123!",
			"store_dockerhub_credentials": true,
			"dockerhub_username":          "testuser",
			"dockerhub_password":          "testpassword",
			"network_acls_default_action": "Allow",
			"allowed_ip_ranges":           []string{},
			"allowed_subnet_ids":          []string{},
			"tags":                        map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, kvOptions)
	terraform.InitAndApply(t, kvOptions)

	outputID := terraform.Output(t, kvOptions, "id")
	assert.NotEmpty(t, outputID, "Key Vault ID should not be empty")
}
