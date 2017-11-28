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

resource "azurerm_virtual_network" "network" {
  name = "foxnet_staging"
  address_space = ["10.0.0.0/16"]
  location = "East US"
  resource_group_name = "${azurerm_resource_group.fox_staging.name}"

  subnet {
    name = "default"
    address_prefix = "10.0.1.0/24"
  }
}
# provider "azure" {
#   publish_settings = "${file("credentials.publishsettings")}"
# }
# 
# resource "azure_virtual_network" "default" {
#   name          = "fox-network"
#   address_space = ["10.1.2.0/24"]
#   location      = "East US"
# 
#   subnet {
#     name           = "subnet1"
#     address_prefix = "10.1.2.0/25"
#   }
# }
