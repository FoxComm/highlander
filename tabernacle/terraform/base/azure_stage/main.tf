variable "ssh_user" {}

variable "ssh_public_key" {}

variable "azure_subscription_id" {}

variable "azure_client_id" {}

variable "azure_client_secret" {}

variable "azure_tenant_id" {}

provider "azurerm" {}

# Create a resource group
resource "azurerm_resource_group" "foxStaging" {
  name     = "foxStaging"
  location = "East US"
}

resource "azurerm_storage_account" "foxImages" {
  name                     = "foximages"
  resource_group_name      = "${azurerm_resource_group.foxStaging.name}"
  location                 = "${azurerm_resource_group.foxStaging.location}"
  account_tier             = "Standard"
  account_replication_type = "GRS"
}

resource "azurerm_network_security_group" "foxnetStagingVpnSG" {
  name                = "foxnetStagingVpnSG"
  location            = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name = "${azurerm_resource_group.foxStaging.name}"

  security_rule {
    name                       = "vpn"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "1194"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "ssh"
    priority                   = 101
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
}

resource "azurerm_network_security_group" "foxnetStagingInternalSG" {
  name                = "foxnetStagingInternalSG"
  location            = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name = "${azurerm_resource_group.foxStaging.name}"

  security_rule {
    name                       = "ssh"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "10.240.2.0/24"
    destination_address_prefix = "*"
  }
}

resource "azurerm_virtual_network" "foxStagingNetwork" {
  name                = "foxnetStaging"
  address_space       = ["10.240.0.0/16"]
  location            = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name = "${azurerm_resource_group.foxStaging.name}"
}

resource "azurerm_subnet" "foxnetStagingVpn" {
  name                 = "foxnetStagingVpnSub"
  resource_group_name  = "${azurerm_resource_group.foxStaging.name}"
  virtual_network_name = "${azurerm_virtual_network.foxStagingNetwork.name}"
  address_prefix       = "10.240.2.0/24"
}

resource "azurerm_subnet" "foxnetStagingInternal" {
  name                 = "foxnetStagingInternalSub"
  resource_group_name  = "${azurerm_resource_group.foxStaging.name}"
  virtual_network_name = "${azurerm_virtual_network.foxStagingNetwork.name}"
  address_prefix       = "10.240.1.0/24"
}

resource "azurerm_public_ip" "foxVpnIp" {
  name                         = "foxVpnIp"
  location                     = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name          = "${azurerm_resource_group.foxStaging.name}"
  public_ip_address_allocation = "Dynamic"
  idle_timeout_in_minutes      = 30
}

resource "azurerm_network_interface" "foxVpnNet" {
  name                      = "foxVpnNet"
  location                  = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name       = "${azurerm_resource_group.foxStaging.name}"
  network_security_group_id = "${azurerm_network_security_group.foxnetStagingVpnSG.id}"

  ip_configuration {
    name                          = "vpnconfiguration1"
    subnet_id                     = "${azurerm_subnet.foxnetStagingVpn.id}"
    private_ip_address_allocation = "dynamic"
    public_ip_address_id          = "${azurerm_public_ip.foxVpnIp.id}"
  }
}

resource "azurerm_virtual_machine" "foxVpn" {
  name                  = "foxVpn"
  location              = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name   = "${azurerm_resource_group.foxStaging.name}"
  network_interface_ids = ["${azurerm_network_interface.foxVpnNet.id}"]
  vm_size               = "Standard_DS1_v2"

  delete_os_disk_on_termination    = true
  delete_data_disks_on_termination = true

  storage_image_reference {
    publisher = "Canonical"
    offer     = "UbuntuServer"
    sku       = "16.04-LTS"
    version   = "latest"
  }

  storage_os_disk {
    name              = "foxVpnOsDisk"
    caching           = "ReadWrite"
    create_option     = "FromImage"
    managed_disk_type = "Standard_LRS"
  }

  os_profile {
    computer_name  = "foxvpn"
    admin_username = "fox"
  }

  os_profile_linux_config {
    disable_password_authentication = true

    ssh_keys {
      path     = "/home/fox/.ssh/authorized_keys"
      key_data = "${var.ssh_public_key}"
    }
  }
}

resource "azurerm_network_interface" "foxApplicationServerNet" {
  name                      = "foxApplicationServerNet"
  location                  = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name       = "${azurerm_resource_group.foxStaging.name}"
  network_security_group_id = "${azurerm_network_security_group.foxnetStagingInternalSG.id}"

  ip_configuration {
    name                          = "appserverconfiguration1"
    subnet_id                     = "${azurerm_subnet.foxnetStagingInternal.id}"
    private_ip_address_allocation = "dynamic"
  }
}

resource "azurerm_virtual_machine" "foxAmigo" {
  name = "foxStagingAmigo${count.index}"
  location              = "${azurerm_resource_group.foxStaging.location}"
  resource_group_name   = "${azurerm_resource_group.foxStaging.name}"
  network_interface_ids = ["${azurerm_network_interface.foxApplicationServerNet.id}"]
  vm_size               = "Standard_DS1_v2"
  count = "3"

  delete_os_disk_on_termination    = true
  delete_data_disks_on_termination = true

  # Replace this with a reference to our base consul image.
  storage_image_reference {
    publisher = "Canonical"
    offer     = "UbuntuServer"
    sku       = "16.04-LTS"
    version   = "latest"
  }

  storage_os_disk {
    name              = "foxAmigoOsDisk${count.index}"
    caching           = "ReadWrite"
    create_option     = "FromImage"
    managed_disk_type = "Standard_LRS"
  }

  os_profile {
    computer_name  = "foxamigo${count.index}"
    admin_username = "fox"
  }

  os_profile_linux_config {
    disable_password_authentication = true

    ssh_keys {
      path     = "/home/fox/.ssh/authorized_keys"
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


# resource "azurerm_container_service" "foxStagingK8s" {
#   name                   = "foxStagingK8s"
#   location               = "${azurerm_resource_group.foxStaging.location}"
#   resource_group_name    = "${azurerm_resource_group.foxStaging.name}"
#   orchestration_platform = "Kubernetes"
# 
#   master_profile {
#     count      = 1
#     dns_prefix = "foxstagingmaster"
#   }
# 
#   linux_profile {
#     admin_username = "fox"
# 
#     ssh_key {
#       key_data = "${var.ssh_public_key}"
#     }
#   }
# 
#   agent_pool_profile {
#     name       = "default"
#     count      = 1
#     dns_prefix = "foxstagingagent"
#     vm_size    = "Standard_A0"
#   }
# 
#   service_principal {
#     client_id     = "dd8eb819-844b-4415-a48f-f21c07a08910"
#     client_secret = "54f5eb93-cf4b-41d4-8f9e-620ca19b6f0e"
#   }
# 
#   diagnostics_profile {
#     enabled = false
#   }
# }

