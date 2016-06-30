variable "ssh_user" {}
variable "ssh_private_key" {}

provider "google"
{
    credentials = "${file("foxcomm-vanilla.json")}"
    project = "foxcommerce-production-shared"
    region = "us-central1"
}

variable "network" {
    default = "vanilla"
}

variable "bucket_location" {
    default = "us"
}

variable "zone" {
    default = "us-central1-a"
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

variable "vpn_image" {
    default = "base-1466895408"
}

module "vanilla_vpn" {
    source = "../../gce/vpn"
    image = "${var.vpn_image}"
    network = "${google_compute_network.vanilla.name}"
}

##############################################
#Consul Cluster
##############################################
variable "consul_cluser_image" {
    default = "consul-server-1466727870"
}

module "vanilla_consul_cluster" {
    source = "../../gce/consul"
    network = "${var.network}"
    datacenter = "${var.network}"
    servers = 3
    image = "${var.consul_cluser_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

##############################################
#Small Production Stack
##############################################

variable "kafka_image" {
    default = "vanilla-kafka-1466725176"
}

variable "db_image" {
    default = "vanilla-db-1467153612"
}

variable "es_image" {
    default = "vanilla-es-1466780580"
}

variable "log_image" {
    default = "vanilla-es-log-1466780565"
}

variable "phoenix_image" {
    default = "vanilla-phoenix-1467315553"
}

variable "greenriver_image" {
    default = "vanilla-green-1466725142"
}

variable "front_image" {
    default = "vanilla-front-1466805340"
}

module "vanilla_stack" {
    source = "../../gce/vanilla_stack"
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
