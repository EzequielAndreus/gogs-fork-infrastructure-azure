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

// TestNetworkingModule tests the networking module
func TestNetworkingModule(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-net-rg-%s", uniqueID)
	vnetName := fmt.Sprintf("test-vnet-%s", uniqueID)
	location := "eastus"

	// Create resource group first
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

	// Create networking resources
	netOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/networking",
		Vars: map[string]interface{}{
			"vnet_name":               vnetName,
			"resource_group_name":     resourceGroupName,
			"location":                location,
			"address_space":           []string{"10.0.0.0/16"},
			"container_subnet_prefix": "10.0.1.0/24",
			"database_subnet_prefix":  "10.0.2.0/24",
			"vm_subnet_prefix":        "10.0.3.0/24",
			"admin_ip_range":          "0.0.0.0/0",
			"tags":                    map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, netOptions)
	terraform.InitAndApply(t, netOptions)

	// Validate outputs
	vnetID := terraform.Output(t, netOptions, "vnet_id")
	containerSubnetID := terraform.Output(t, netOptions, "container_subnet_id")
	databaseSubnetID := terraform.Output(t, netOptions, "database_subnet_id")
	vmSubnetID := terraform.Output(t, netOptions, "vm_subnet_id")
	vmNsgID := terraform.Output(t, netOptions, "vm_nsg_id")

	// Assertions
	assert.NotEmpty(t, vnetID, "VNet ID should not be empty")
	assert.NotEmpty(t, containerSubnetID, "Container subnet ID should not be empty")
	assert.NotEmpty(t, databaseSubnetID, "Database subnet ID should not be empty")
	assert.NotEmpty(t, vmSubnetID, "VM subnet ID should not be empty")
	assert.NotEmpty(t, vmNsgID, "VM NSG ID should not be empty")

	// Verify VNet exists in Azure
	subscriptionID := azure.GetTargetAzureSubscription(t)
	exists := azure.VirtualNetworkExists(t, vnetName, resourceGroupName, subscriptionID)
	assert.True(t, exists, "Virtual network should exist in Azure")
}

// TestNetworkingModuleSubnetConfiguration tests subnet CIDR configurations
func TestNetworkingModuleSubnetConfiguration(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-net-sub-rg-%s", uniqueID)
	vnetName := fmt.Sprintf("test-vnet-sub-%s", uniqueID)
	location := "westus2"

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

	// Create networking with custom CIDR ranges
	netOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/networking",
		Vars: map[string]interface{}{
			"vnet_name":               vnetName,
			"resource_group_name":     resourceGroupName,
			"location":                location,
			"address_space":           []string{"172.16.0.0/16"},
			"container_subnet_prefix": "172.16.10.0/24",
			"database_subnet_prefix":  "172.16.20.0/24",
			"vm_subnet_prefix":        "172.16.30.0/24",
			"admin_ip_range":          "10.0.0.0/8",
			"tags":                    map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, netOptions)
	terraform.InitAndApply(t, netOptions)

	vnetID := terraform.Output(t, netOptions, "vnet_id")
	assert.NotEmpty(t, vnetID, "VNet ID should not be empty")
}
