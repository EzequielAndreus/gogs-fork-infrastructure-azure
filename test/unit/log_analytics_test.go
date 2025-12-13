package test

import (
	"fmt"
	"strings"
	"testing"

	"github.com/gruntwork-io/terratest/modules/random"
	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
)

// TestLogAnalyticsModule tests the log-analytics module
func TestLogAnalyticsModule(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-la-rg-%s", uniqueID)
	workspaceName := fmt.Sprintf("testla%s", uniqueID)
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

	// Create Log Analytics workspace
	laOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/log-analytics",
		Vars: map[string]interface{}{
			"workspace_name":            workspaceName,
			"resource_group_name":       resourceGroupName,
			"location":                  location,
			"sku":                       "PerGB2018",
			"retention_in_days":         30,
			"enable_container_insights": false,
			"enable_sql_analytics":      false,
			"tags":                      map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, laOptions)
	terraform.InitAndApply(t, laOptions)

	// Validate outputs
	workspaceID := terraform.Output(t, laOptions, "id")
	workspaceGUID := terraform.Output(t, laOptions, "workspace_id")
	primaryKey := terraform.Output(t, laOptions, "primary_shared_key")
	outputName := terraform.Output(t, laOptions, "name")

	// Assertions
	assert.NotEmpty(t, workspaceID, "Workspace ID should not be empty")
	assert.NotEmpty(t, workspaceGUID, "Workspace GUID should not be empty")
	assert.NotEmpty(t, primaryKey, "Primary shared key should not be empty")
	assert.Equal(t, workspaceName, outputName, "Workspace name should match")
}

// TestLogAnalyticsModuleWithContainerInsights tests log analytics with Container Insights
func TestLogAnalyticsModuleWithContainerInsights(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-la-ci-rg-%s", uniqueID)
	workspaceName := fmt.Sprintf("testlaci%s", uniqueID)
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

	// Create Log Analytics with Container Insights
	laOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/log-analytics",
		Vars: map[string]interface{}{
			"workspace_name":            workspaceName,
			"resource_group_name":       resourceGroupName,
			"location":                  location,
			"sku":                       "PerGB2018",
			"retention_in_days":         60,
			"enable_container_insights": true,
			"enable_sql_analytics":      false,
			"tags":                      map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, laOptions)
	terraform.InitAndApply(t, laOptions)

	workspaceID := terraform.Output(t, laOptions, "id")
	assert.NotEmpty(t, workspaceID, "Workspace ID should not be empty")
}

// TestLogAnalyticsModuleWithAllSolutions tests log analytics with all solutions enabled
func TestLogAnalyticsModuleWithAllSolutions(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-la-all-rg-%s", uniqueID)
	workspaceName := fmt.Sprintf("testlaall%s", uniqueID)
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

	// Create Log Analytics with all solutions
	laOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/log-analytics",
		Vars: map[string]interface{}{
			"workspace_name":            workspaceName,
			"resource_group_name":       resourceGroupName,
			"location":                  location,
			"sku":                       "PerGB2018",
			"retention_in_days":         90,
			"enable_container_insights": true,
			"enable_sql_analytics":      true,
			"tags":                      map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, laOptions)
	terraform.InitAndApply(t, laOptions)

	workspaceID := terraform.Output(t, laOptions, "id")
	assert.NotEmpty(t, workspaceID, "Workspace ID should not be empty")
}
