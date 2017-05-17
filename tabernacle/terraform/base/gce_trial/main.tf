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
resource "google_compute_network" "trial" {
  name       = "${var.network}"
  ipv4_range = "10.0.0.0/16"
}

resource "google_compute_firewall" "trial_web" {
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

resource "google_compute_firewall" "trial_ssh" {
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

resource "google_compute_firewall" "trial_internal" {
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
module "trial_vpn" {
  source  = "../../modules/gce/vpn"
  image   = "${var.vpn_image}"
  network = "${google_compute_network.trial.name}"
}

##############################################
# Production Cluster
##############################################
module "trial_production" {
  source             = "../../modules/gce/trial"
  ssh_user           = "${var.ssh_user}"
  ssh_private_key    = "${var.ssh_private_key}"
  network            = "${google_compute_network.trial.name}"
  datacenter         = "trial"
  amigo_image        = "trial-amigo-170516-231235"
  logstash_image     = ""
  database_image     = "trial-database-170516-235144"
  search_image       = "trial-search-170517-000959"
  frontend_image     = "trial-frontend-170517-001738"
}

# ##############################################
# # DNS Records
# ##############################################
# provider "dnsimple" {
#   token   = "${var.dnsimple_token}"
#   account = "${var.dnsimple_account}"
# }


# resource "dnsimple_record" "docker-registry-dns-record" {
#   domain = "foxcommerce.com"
#   name   = "docker-trial"
#   value  = "${module.trial_production.amigo_address}"
#   type   = "A"
#   ttl    = 3600
# }


# resource "dnsimple_record" "frontend-dns-record" {
#   domain = "foxcommerce.com"
#   name   = "trial"
#   value  = "${module.trial_production.frontend_address}"
#   type   = "A"
#   ttl    = 3600
# }

