# Azure Container Instance Module
# Docker service for running containers from DockerHub

resource "azurerm_container_group" "main" {
  name                = var.container_group_name
  location            = var.location
  resource_group_name = var.resource_group_name
  ip_address_type     = var.ip_address_type
  dns_name_label      = var.dns_name_label
  os_type             = var.os_type
  restart_policy      = var.restart_policy

  # DockerHub image credentials
  dynamic "image_registry_credential" {
    for_each = var.dockerhub_username != "" ? [1] : []
    content {
      server   = "index.docker.io"
      username = var.dockerhub_username
      password = var.dockerhub_password
    }
  }

  container {
    name   = var.container_name
    image  = var.docker_image
    cpu    = var.cpu
    memory = var.memory

    ports {
      port     = var.container_port
      protocol = "TCP"
    }

    dynamic "environment_variables" {
      for_each = var.environment_variables
      content {
        name  = environment_variables.key
        value = environment_variables.value
      }
    }

    dynamic "secure_environment_variables" {
      for_each = var.secure_environment_variables
      content {
        name  = secure_environment_variables.key
        value = secure_environment_variables.value
      }
    }

    dynamic "volume" {
      for_each = var.volumes
      content {
        name                 = volume.value.name
        mount_path           = volume.value.mount_path
        read_only            = lookup(volume.value, "read_only", false)
        empty_dir            = lookup(volume.value, "empty_dir", false)
        storage_account_name = lookup(volume.value, "storage_account_name", null)
        storage_account_key  = lookup(volume.value, "storage_account_key", null)
        share_name           = lookup(volume.value, "share_name", null)
      }
    }
  }

  diagnostics {
    log_analytics {
      workspace_id  = var.log_analytics_workspace_id
      workspace_key = var.log_analytics_workspace_key
    }
  }

  tags = var.tags
}
