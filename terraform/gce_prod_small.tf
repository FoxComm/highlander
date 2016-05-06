
variable "ssh_user" {} 
variable "ssh_private_key" {} 

##############################################
# Network
##############################################

resource "google_compute_network" "test" {
  name       = "prodsmall"
  ipv4_range = "10.0.0.0/16"
}

##############################################
# Vpn
##############################################

variable "vpn_image" { 
    default = "base-1462485956"
}

module "vpn" {
    source = "./gce/vpn"
    image = "${var.vpn_image}"
    network = "${google_compute_network.test.name}"
}

