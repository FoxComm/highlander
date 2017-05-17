variable "ssh_user" {}

variable "ssh_private_key" {}

variable "dnsimple_account" {}

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
resource "google_compute_network" "ark" {
  name       = "${var.network}"
  ipv4_range = "10.0.0.0/16"
}

resource "google_compute_firewall" "ark_web" {
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

resource "google_compute_firewall" "ark_ssh" {
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

resource "google_compute_firewall" "ark_internal" {
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
module "ark_vpn" {
  source  = "../../modules/gce/vpn"
  image   = "${var.vpn_image}"
  network = "${google_compute_network.ark.name}"
}

##############################################
# Production Cluster
##############################################
module "ark_production" {
  source             = "../../modules/gce/ark"
  ssh_user           = "${var.ssh_user}"
  ssh_private_key    = "${var.ssh_private_key}"
  network            = "${google_compute_network.ark.name}"
  datacenter         = "ark"
  amigo_image        = "trial-amigo-170517-172258"
  logstash_image     = "trial-logstash-170517-173120"
  database_image     = "trial-database-170516-235144"
  search_image       = "trial-search-170517-000959"
  frontend_image     = "trial-frontend-170517-001738"
}
