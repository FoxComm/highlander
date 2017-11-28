variable "ssh_user" {}

variable "ssh_private_key" {}

variable "azure_subscription_id" {}

variable "azure_client_id" {}

variable "azure_client_secret" {}

variable "azure_tenant_id" {}

provider "azurerm" {
  subscription_id = "${var.azure_subscription_id}"
  client_id       = "${var.azure_client_id}"
  client_secret   = "${var.azure_client_secret}"
  tenant_id       = "${var.azure_tenant_id}"
}

# Create a resource group
resource "azurerm_resource_group" "fox_staging" {
  name      = "fox_staging"
  location  = "East US"
}

resource "azurerm_container_registry" "test" {
  name                = "fox_staging_registry"
  resource_group_name = "${azurerm_resource_group.fox_staging.name}"
  location            = "${azurerm_resource_group.fox_staging.location}"
  admin_enabled       = true
  sku                 = "Standard"
}
