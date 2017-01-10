variable "ssh_user" {}

variable "ssh_private_key" {}

variable "dnsimple_email" {}

variable "dnsimple_token" {}

variable "account_file" {}

variable "gce_project" {}

variable "region" {}

variable "zone" {}

variable "vpn_image" {}

variable "network" {}

provider "google" {
  credentials = "${file(var.account_file)}"
  project     = "${var.gce_project}"
  region      = "${var.region}"
}

##############################################
# Network
##############################################
resource "google_compute_network" "tpg" {
  name       = "${var.network}"
  ipv4_range = "10.0.0.0/16"
}

resource "google_compute_firewall" "tpg_web" {
  name    = "${var.network}-web"
  network = "${var.network}"

  allow {
    protocol = "icmp"
  }

  allow {
    protocol = "tcp"
    ports    = ["80", "443"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["http-server", "https-server"]
}

resource "google_compute_firewall" "tpg_ssh" {
  name    = "${var.network}-ssh"
  network = "${var.network}"

  allow {
    protocol = "icmp"
  }

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["ssh"]
}

resource "google_compute_firewall" "tpg_internal" {
  name    = "${var.network}-internal"
  network = "${var.network}"

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
# VPN
##############################################
module "tpg_vpn" {
  source  = "../../modules/gce/vpn"
  image   = "${var.vpn_image}"
  network = "${google_compute_network.tpg.name}"
}

##############################################
# Staging Cluster
##############################################
module "tpg_staging" {
  source          = "../../modules/gce/tinyproduction"
  ssh_user        = "${var.ssh_user}"
  ssh_private_key = "${var.ssh_private_key}"
  network         = "${google_compute_network.tpg.name}"
  datacenter      = "tpg-stage"
  amigo_image     = "base-amigo-161104-095319"
  backend_image   = "base-backend-161104-105155"
  frontend_image  = "base-frontend-161104-105130"
}

##############################################
# Production Cluster
##############################################
module "tpg_production" {
  source          = "../../modules/gce/tinyproduction"
  ssh_user        = "${var.ssh_user}"
  ssh_private_key = "${var.ssh_private_key}"
  network         = "${google_compute_network.tpg.name}"
  datacenter      = "tpg"
  amigo_image     = "base-amigo-161104-095319"
  backend_image   = "base-backend-161104-105155"
  frontend_image  = "base-frontend-161104-105130"
}

##############################################
# DNS Records
##############################################
provider "dnsimple" {
  token = "${var.dnsimple_token}"
  email = "${var.dnsimple_email}"
}

resource "dnsimple_record" "docker-registry-dns-record" {
  domain = "foxcommerce.com"
  name   = "docker-tpg"
  value  = "${module.tpg_production.amigo_address}"
  type   = "A"
  ttl    = 3600
}

resource "dnsimple_record" "frontend-dns-record" {
  domain = "foxcommerce.com"
  name   = "tpg"
  value  = "${module.tpg_production.frontend_address}"
  type   = "A"
  ttl    = 3600
}
