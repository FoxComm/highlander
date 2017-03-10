variable "azure_subscription_id" {}

variable "azure_tenant_id" {}

variable "azure_client_id" {}

variable "azure_client_secret" {}

provider "azurerm" {
  subscription_id = "${var.azure_subscription_id}"
  client_id       = "${var.azure_client_id}"
  client_secret   = "${var.azure_client_secret}"
  tenant_id       = "${var.azure_tenant_id}"
}

# create a resource group
resource "azurerm_resource_group" "helloterraform" {
  name     = "terraformtest"
  location = "West US"
}

# Create a virtual network in the web_servers resource group
resource "azurerm_virtual_network" "helloterraformnetwork" {
  name                = "acctvn"
  address_space       = ["10.0.0.0/16"]
  location            = "West US"
  resource_group_name = "${azurerm_resource_group.helloterraform.name}"
}

# create subnet
resource "azurerm_subnet" "helloterraformsubnet" {
  name                 = "acctsub"
  resource_group_name  = "${azurerm_resource_group.helloterraform.name}"
  virtual_network_name = "${azurerm_virtual_network.helloterraformnetwork.name}"
  address_prefix       = "10.0.2.0/24"
}

# create public IP
resource "azurerm_public_ip" "helloterraformips" {
  name                         = "terraformtestip"
  location                     = "West US"
  resource_group_name          = "${azurerm_resource_group.helloterraform.name}"
  public_ip_address_allocation = "dynamic"

  tags {
    environment = "TerraformDemo"
  }
}

# create network interface
resource "azurerm_network_interface" "helloterraformnic" {
  name                = "tfni"
  location            = "West US"
  resource_group_name = "${azurerm_resource_group.helloterraform.name}"

  ip_configuration {
    name                          = "testconfiguration1"
    subnet_id                     = "${azurerm_subnet.helloterraformsubnet.id}"
    private_ip_address_allocation = "static"
    private_ip_address            = "10.0.2.5"
    public_ip_address_id          = "${azurerm_public_ip.helloterraformips.id}"
  }
}

# create storage account
resource "azurerm_storage_account" "pavelspikestorage2017" {
  name                = "pavelspikestorage2017"
  resource_group_name = "${azurerm_resource_group.helloterraform.name}"
  location            = "westus"
  account_type        = "Standard_LRS"

  tags {
    environment = "staging"
  }
}

# create storage container
resource "azurerm_storage_container" "pavelspikestorage2017storagecontainer" {
  name                  = "vhd"
  resource_group_name   = "${azurerm_resource_group.helloterraform.name}"
  storage_account_name  = "${azurerm_storage_account.pavelspikestorage2017.name}"
  container_access_type = "private"
  depends_on            = ["azurerm_storage_account.pavelspikestorage2017"]
}

# create virtual machine
resource "azurerm_virtual_machine" "helloterraformvm" {
  name                  = "terraformvm"
  location              = "West US"
  resource_group_name   = "${azurerm_resource_group.helloterraform.name}"
  network_interface_ids = ["${azurerm_network_interface.helloterraformnic.id}"]
  vm_size               = "Standard_A0"

  storage_image_reference {
    publisher = "Canonical"
    offer     = "UbuntuServer"
    sku       = "16.10"
    version   = "latest"
  }

  storage_os_disk {
    name          = "myosdisk"
    vhd_uri       = "${azurerm_storage_account.pavelspikestorage2017.primary_blob_endpoint}${azurerm_storage_container.pavelspikestorage2017storagecontainer.name}/myosdisk.vhd"
    caching       = "ReadWrite"
    create_option = "FromImage"
  }

  os_profile {
    computer_name  = "hostname"
    admin_username = "testadmin"
    admin_password = "Password1234!"
  }

  os_profile_linux_config {
    disable_password_authentication = false
  }

  tags {
    environment = "staging"
  }
}
