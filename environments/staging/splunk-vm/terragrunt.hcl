# Staging Splunk VM
# Azure Virtual Machine for Splunk monitoring (AWS EC2 equivalent)

include "root" {
  path = find_in_parent_folders()
}

include "env" {
  path   = find_in_parent_folders("env.hcl")
  expose = true
}

terraform {
  source = "${get_repo_root()}/modules/virtual-machine"
}

dependency "resource_group" {
  config_path = "../resource-group"

  mock_outputs = {
    resource_group_name     = "mock-rg"
    resource_group_location = "eastus"
  }
}

dependency "networking" {
  config_path = "../networking"

  mock_outputs = {
    vm_subnet_id = "mock-subnet-id"
    vm_nsg_id    = "mock-nsg-id"
  }
}

inputs = {
  vm_name             = "vm-stg-splunk"
  location            = dependency.resource_group.outputs.resource_group_location
  resource_group_name = dependency.resource_group.outputs.resource_group_name
  subnet_id           = dependency.networking.outputs.vm_subnet_id
  network_security_group_id = dependency.networking.outputs.vm_nsg_id
  
  # VM configuration
  vm_size        = "Standard_D4s_v3"  # 4 vCPUs, 16 GB RAM
  admin_username = "splunkadmin"
  ssh_public_key = get_env("TF_VAR_splunk_ssh_public_key", "")
  
  # Network
  create_public_ip = true
  
  # OS Disk
  os_disk_type    = "Premium_LRS"
  os_disk_size_gb = 128
  
  # Image (Ubuntu 22.04 LTS)
  image_publisher = "Canonical"
  image_offer     = "0001-com-ubuntu-server-jammy"
  image_sku       = "22_04-lts-gen2"
  image_version   = "latest"
  
  # Data Disk for Splunk
  create_data_disk  = true
  data_disk_type    = "Premium_LRS"
  data_disk_size_gb = 256
  
  # Cloud-init script for initial setup
  custom_data = <<-EOF
#!/bin/bash
# Initial system setup
apt-get update
apt-get install -y wget curl apt-transport-https

# Mount data disk
mkfs.ext4 /dev/sdc
mkdir -p /opt/splunk
mount /dev/sdc /opt/splunk
echo '/dev/sdc /opt/splunk ext4 defaults 0 2' >> /etc/fstab

# Download and install Splunk (placeholder - actual installation requires license)
# wget -O splunk.deb 'https://download.splunk.com/products/splunk/releases/9.1.2/linux/splunk-9.1.2-amd64.deb'
# dpkg -i splunk.deb

echo "VM setup complete. Splunk installation requires manual configuration."
EOF
  
  tags = include.env.inputs.tags
}
