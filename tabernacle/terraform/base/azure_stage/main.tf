variable "ssh_user" {}

variable "ssh_public_key" {}

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
resource "azurerm_resource_group" "foxStaging" {
  name      = "foxStaging"
  location  = "East US"
}

resource "azurerm_virtual_network" "network" {
  name                = "foxnetStaging"
  address_space       = ["10.240.0.0/16"]
  location            = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name = "${azurerm_resource_group.foxStaging.name}"
}

resource "azurerm_subnet" "foxnetStagingInternal" {
  name = "foxnetStagingInternalSub"
  resource_group_name = "${azurerm_resource_group.foxStaging.name}"
  virtual_network_name = "${azurerm_virtual_network.network.name}"
  address_prefix = "10.240.1.0/24"
}

resource "azurerm_public_ip" "foxVpnIp" {
  name = "foxVpnIp"
  location = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name = "${azurerm_resource_group.foxStaging.name}"
  public_ip_address_allocation = "Dynamic"
  idle_timeout_in_minutes = 30
}

resource "azurerm_network_interface" "foxVpnNet" {
  name = "foxVpnNet"
  location = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name = "${azurerm_resource_group.foxStaging.name}"

  ip_configuration {
    name = "vpnconfiguration1"
    subnet_id = "${azurerm_subnet.foxnetStagingInternal.id}"
    private_ip_address_allocation = "dynamic"
    public_ip_address_id = "${azurerm_public_ip.foxVpnIp.id}"
  }
}

resource "azurerm_virtual_machine" "foxVpn" {
  name = "foxVpn"
  location = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name = "${azurerm_resource_group.foxStaging.name}"
  network_interface_ids = ["${azurerm_network_interface.foxVpnNet.id}"]
  vm_size = "Standard_DS1_v2"

  delete_os_disk_on_termination = true
  delete_data_disks_on_termination = true

  storage_image_reference {
    publisher = "Canonical"
    offer = "UbuntuServer"
    sku = "16.04-LTS"
    version = "latest"
  }

  storage_os_disk {
    name = "foxVpnOsDisk"
    caching = "ReadWrite"
    create_option = "FromImage"
    managed_disk_type = "Standard_LRS"
  }

  os_profile {
    computer_name = "foxvpn"
    admin_username = "fox"
  }

  os_profile_linux_config {
    disable_password_authentication = true
    ssh_keys {
      path = "/home/fox/.ssh/authorized_keys"
      key_data = "${var.ssh_public_key}"
    }
  }
}

resource "azurerm_container_registry" "foxStagingRegistry" {
  name                = "foxStagingRegistry"
  resource_group_name = "${azurerm_resource_group.foxStaging.name}"
  location            = "${azurerm_resource_group.foxStaging.location}"
  admin_enabled       = true
  sku                 = "Standard"
}
