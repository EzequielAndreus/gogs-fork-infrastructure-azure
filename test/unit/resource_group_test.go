package test

import (
	"fmt"
	"strings"
	"testing"

	"github.com/gruntwork-io/terratest/modules/azure"
	"github.com/gruntwork-io/terratest/modules/random"
	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
)

// TestResourceGroupModule tests the resource-group module
func TestResourceGroupModule(t *testing.T) {
	t.Parallel()

	// Generate a random suffix to ensure unique resource names
	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-rg-%s", uniqueID)
	location := "eastus"

	// Terraform options for the module
	terraformOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/resource-group",

		Vars: map[string]interface{}{
			"resource_group_name": resourceGroupName,
			"location":            location,
			"tags": map[string]string{
				"Environment": "test",
				"ManagedBy":   "terratest",
				"TestID":      uniqueID,
			},
		},
	})

	// Defer destruction of resources
	defer terraform.Destroy(t, terraformOptions)

	// Create the resources
	terraform.InitAndApply(t, terraformOptions)

	// Validate outputs
	outputName := terraform.Output(t, terraformOptions, "name")
	outputLocation := terraform.Output(t, terraformOptions, "location")
	outputID := terraform.Output(t, terraformOptions, "id")

	// Assertions
	assert.Equal(t, resourceGroupName, outputName, "Resource group name should match")
	assert.Equal(t, location, outputLocation, "Location should match")
	assert.NotEmpty(t, outputID, "Resource group ID should not be empty")

	// Verify the resource group exists in Azure
	subscriptionID := azure.GetTargetAzureSubscription(t)
	exists := azure.ResourceGroupExists(t, resourceGroupName, subscriptionID)
	assert.True(t, exists, "Resource group should exist in Azure")
}

// TestResourceGroupModuleWithCustomTags tests resource group with custom tags
func TestResourceGroupModuleWithCustomTags(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-rg-tags-%s", uniqueID)

	terraformOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/resource-group",

		Vars: map[string]interface{}{
			"resource_group_name": resourceGroupName,
			"location":            "westus2",
			"tags": map[string]string{
				"Environment": "test",
				"Project":     "infrastructure-test",
				"CostCenter":  "testing",
			},
		},
	})

	defer terraform.Destroy(t, terraformOptions)
	terraform.InitAndApply(t, terraformOptions)

	outputName := terraform.Output(t, terraformOptions, "name")
	assert.Equal(t, resourceGroupName, outputName)
}
