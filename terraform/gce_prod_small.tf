
variable "ssh_user" {} 
variable "ssh_private_key" {} 

##############################################
# Network
##############################################

resource "google_compute_network" "prodsmall" {
  name       = "prodsmall"
  ipv4_range = "10.0.0.0/16"
} 

resource "google_compute_firewall" "web" {
  name    = "web"
  network = "${google_compute_network.prodsmall.name}"

  allow {
    protocol = "icmp"
  }

  allow {
    protocol = "tcp"
    ports    = ["80", "443"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags = ["http-server", "https-server"]
}

resource "google_compute_firewall" "ssh" {
  name    = "ssh"
  network = "${google_compute_network.prodsmall.name}"

  allow {
    protocol = "icmp"
  }

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags = ["ssh"]
}

resource "google_compute_firewall" "internal" {
  name    = "internal"
  network = "${google_compute_network.prodsmall.name}"

  allow {
    protocol = "icmp"
  }

  allow {
    protocol = "tcp"
    ports    = ["1-65535"]
  }

  allow {
    protocol = "udp"
    ports    = ["1-65535"]
  }

  source_ranges = ["10.0.0.0/16"]
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
    network = "${google_compute_network.prodsmall.name}"
}

