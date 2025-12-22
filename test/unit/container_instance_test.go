package test

import (
	"fmt"
	"strings"
	"testing"

	"github.com/gruntwork-io/terratest/modules/random"
	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
)

// TestContainerInstanceModule tests the container-instance module
func TestContainerInstanceModule(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-aci-rg-%s", uniqueID)
	containerGroupName := fmt.Sprintf("test-aci-%s", uniqueID)
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

	// Create Log Analytics workspace for diagnostics
	laOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/log-analytics",
		Vars: map[string]interface{}{
			"workspace_name":            fmt.Sprintf("testla%s", uniqueID),
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

	workspaceID := terraform.Output(t, laOptions, "workspace_id")
	workspaceKey := terraform.Output(t, laOptions, "primary_shared_key")

	// Create container instance
	aciOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/container-instance",
		Vars: map[string]interface{}{
			"container_group_name":         containerGroupName,
			"resource_group_name":          resourceGroupName,
			"location":                     location,
			"container_name":               "test-container",
			"docker_image":                 "nginx:latest",
			"cpu":                          0.5,
			"memory":                       0.5,
			"container_port":               80,
			"ip_address_type":              "Public",
			"dns_name_label":               fmt.Sprintf("test-aci-%s", uniqueID),
			"os_type":                      "Linux",
			"restart_policy":               "Always",
			"dockerhub_username":           "",
			"dockerhub_password":           "",
			"environment_variables":        map[string]string{},
			"secure_environment_variables": map[string]string{},
			"volumes":                      []map[string]interface{}{},
			"log_analytics_workspace_id":   workspaceID,
			"log_analytics_workspace_key":  workspaceKey,
			"tags":                         map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, aciOptions)
	terraform.InitAndApply(t, aciOptions)

	// Validate outputs
	containerID := terraform.Output(t, aciOptions, "id")
	fqdn := terraform.Output(t, aciOptions, "fqdn")
	ipAddress := terraform.Output(t, aciOptions, "ip_address")

	// Assertions
	assert.NotEmpty(t, containerID, "Container ID should not be empty")
	assert.NotEmpty(t, fqdn, "FQDN should not be empty")
	assert.NotEmpty(t, ipAddress, "IP address should not be empty")
	assert.Contains(t, fqdn, uniqueID, "FQDN should contain the unique ID")
}

// TestContainerInstanceModuleWithEnvVars tests container with environment variables
func TestContainerInstanceModuleWithEnvVars(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-aci-env-rg-%s", uniqueID)
	containerGroupName := fmt.Sprintf("test-aci-env-%s", uniqueID)
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
			"workspace_name":            fmt.Sprintf("testlaenv%s", uniqueID),
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

	workspaceID := terraform.Output(t, laOptions, "workspace_id")
	workspaceKey := terraform.Output(t, laOptions, "primary_shared_key")

	// Create container with environment variables
	aciOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/container-instance",
		Vars: map[string]interface{}{
			"container_group_name": containerGroupName,
			"resource_group_name":  resourceGroupName,
			"location":             location,
			"container_name":       "test-container",
			"docker_image":         "nginx:latest",
			"cpu":                  0.5,
			"memory":               0.5,
			"container_port":       80,
			"ip_address_type":      "Public",
			"dns_name_label":       fmt.Sprintf("test-aci-env-%s", uniqueID),
			"os_type":              "Linux",
			"restart_policy":       "Always",
			"dockerhub_username":   "",
			"dockerhub_password":   "",
			"environment_variables": map[string]string{
				"APP_ENV":  "test",
				"APP_NAME": "test-app",
			},
			"secure_environment_variables": map[string]string{},
			"volumes":                      []map[string]interface{}{},
			"log_analytics_workspace_id":   workspaceID,
			"log_analytics_workspace_key":  workspaceKey,
			"tags":                         map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, aciOptions)
	terraform.InitAndApply(t, aciOptions)

	containerID := terraform.Output(t, aciOptions, "id")
	assert.NotEmpty(t, containerID, "Container ID should not be empty")
}
