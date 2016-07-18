variable "ssh_user" {}
variable "ssh_private_key" {}

variable "account_file" {}
variable "gce_project" {}
variable "region" {}
variable "network" {}
variable "bucket_location" {}
variable "zone" {}
variable "vpn_image" {}
variable "consul_cluster_image" {}
variable "kafka_image" {}
variable "db_image" {}
variable "es_image" {}
variable "log_image" {}
variable "phoenix_image" {}
variable "greenriver_image" {}
variable "front_image" {}

provider "google"
{
    credentials = "${file(var.account_file)}"
    project = "${var.gce_project}"
    region = "${var.region}"
}

##############################################
# Network
##############################################
resource "google_compute_network" "vanilla" {
  name       = "${var.network}"
  ipv4_range = "10.0.0.0/16"
}

resource "google_compute_firewall" "vanilla_web" {
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
  target_tags = ["http-server", "https-server"]
}

resource "google_compute_firewall" "vanilla_ssh" {
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
  target_tags = ["ssh"]
}

resource "google_compute_firewall" "vanilla_internal" {
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
# Vpn
##############################################
module "vanilla_vpn" {
    source = "../../modules/gce/vpn"
    image = "${var.vpn_image}"
    network = "${google_compute_network.vanilla.name}"
}

##############################################
# Consul Cluster
##############################################
module "vanilla_consul_cluster" {
    source = "../../modules/gce/consul"
    network = "${var.network}"
    datacenter = "${var.network}"
    servers = 3
    image = "${var.consul_cluster_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

##############################################
# Small Production Stack
##############################################
module "vanilla_stack" {
    source = "../../modules/gce/vanilla_stack"
    zone = "${var.zone}"
    network = "${var.network}"
    datacenter = "${var.network}"
    kafka_image = "${var.kafka_image}"
    db_image = "${var.db_image}"
    es_image = "${var.es_image}"
    log_image = "${var.log_image}"
    phoenix_image = "${var.phoenix_image}"
    greenriver_image = "${var.greenriver_image}"
    front_image = "${var.front_image}"
    consul_leader = "${module.vanilla_consul_cluster.leader}"
    bucket_location = "${var.bucket_location}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
