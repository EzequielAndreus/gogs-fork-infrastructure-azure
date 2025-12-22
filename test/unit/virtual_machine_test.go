package test

import (
	"crypto/rand"
	"crypto/rsa"
	"fmt"
	"strings"
	"testing"

	"github.com/gruntwork-io/terratest/modules/random"
	"github.com/gruntwork-io/terratest/modules/terraform"
	"github.com/stretchr/testify/assert"
	"golang.org/x/crypto/ssh"
)

// generateSSHKeyPair generates an RSA SSH key pair for testing
func generateSSHKeyPair(t *testing.T) string {
	privateKey, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		t.Fatalf("Failed to generate private key: %v", err)
	}

	publicKey, err := ssh.NewPublicKey(&privateKey.PublicKey)
	if err != nil {
		t.Fatalf("Failed to generate public key: %v", err)
	}

	return string(ssh.MarshalAuthorizedKey(publicKey))
}

// TestVirtualMachineModule tests the virtual-machine module
func TestVirtualMachineModule(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-vm-rg-%s", uniqueID)
	vmName := fmt.Sprintf("testvm%s", uniqueID)
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

	// Create networking
	vnetName := fmt.Sprintf("test-vnet-%s", uniqueID)
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

	vmSubnetID := terraform.Output(t, netOptions, "vm_subnet_id")
	vmNsgID := terraform.Output(t, netOptions, "vm_nsg_id")

	// Generate SSH key for testing
	sshPublicKey := generateSSHKeyPair(t)

	// Create VM
	vmOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/virtual-machine",
		Vars: map[string]interface{}{
			"vm_name":                   vmName,
			"resource_group_name":       resourceGroupName,
			"location":                  location,
			"vm_size":                   "Standard_B1s",
			"admin_username":            "testadmin",
			"ssh_public_key":            sshPublicKey,
			"subnet_id":                 vmSubnetID,
			"network_security_group_id": vmNsgID,
			"create_public_ip":          true,
			"create_data_disk":          false,
			"os_disk_type":              "Standard_LRS",
			"os_disk_size_gb":           30,
			"image_publisher":           "Canonical",
			"image_offer":               "0001-com-ubuntu-server-jammy",
			"image_sku":                 "22_04-lts",
			"image_version":             "latest",
			"custom_data":               "",
			"tags":                      map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, vmOptions)
	terraform.InitAndApply(t, vmOptions)

	// Validate outputs
	vmID := terraform.Output(t, vmOptions, "vm_id")
	privateIP := terraform.Output(t, vmOptions, "private_ip")
	publicIP := terraform.Output(t, vmOptions, "public_ip")
	identityPrincipalID := terraform.Output(t, vmOptions, "identity_principal_id")

	// Assertions
	assert.NotEmpty(t, vmID, "VM ID should not be empty")
	assert.NotEmpty(t, privateIP, "Private IP should not be empty")
	assert.NotEmpty(t, publicIP, "Public IP should not be empty")
	assert.NotEmpty(t, identityPrincipalID, "Managed identity principal ID should not be empty")
}

// TestVirtualMachineModuleWithDataDisk tests VM with attached data disk
func TestVirtualMachineModuleWithDataDisk(t *testing.T) {
	t.Parallel()

	uniqueID := strings.ToLower(random.UniqueId())
	resourceGroupName := fmt.Sprintf("test-vm-dd-rg-%s", uniqueID)
	vmName := fmt.Sprintf("testvmdd%s", uniqueID)
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

	// Create networking
	vnetName := fmt.Sprintf("test-vnet-dd-%s", uniqueID)
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

	vmSubnetID := terraform.Output(t, netOptions, "vm_subnet_id")
	vmNsgID := terraform.Output(t, netOptions, "vm_nsg_id")

	sshPublicKey := generateSSHKeyPair(t)

	// Create VM with data disk
	vmOptions := terraform.WithDefaultRetryableErrors(t, &terraform.Options{
		TerraformDir: "../modules/virtual-machine",
		Vars: map[string]interface{}{
			"vm_name":                   vmName,
			"resource_group_name":       resourceGroupName,
			"location":                  location,
			"vm_size":                   "Standard_B1s",
			"admin_username":            "testadmin",
			"ssh_public_key":            sshPublicKey,
			"subnet_id":                 vmSubnetID,
			"network_security_group_id": vmNsgID,
			"create_public_ip":          false,
			"create_data_disk":          true,
			"data_disk_size_gb":         32,
			"data_disk_type":            "Standard_LRS",
			"os_disk_type":              "Standard_LRS",
			"os_disk_size_gb":           30,
			"image_publisher":           "Canonical",
			"image_offer":               "0001-com-ubuntu-server-jammy",
			"image_sku":                 "22_04-lts",
			"image_version":             "latest",
			"custom_data":               "",
			"tags":                      map[string]string{"Environment": "test"},
		},
	})

	defer terraform.Destroy(t, vmOptions)
	terraform.InitAndApply(t, vmOptions)

	vmID := terraform.Output(t, vmOptions, "vm_id")
	assert.NotEmpty(t, vmID, "VM ID should not be empty")
}
