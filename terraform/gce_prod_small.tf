
variable "ssh_user" {} 
variable "ssh_private_key" {} 
variable "network" { 
    default = "prodsmall"
}
variable "zone" {
    default = "us-central1-a"
}

##############################################
# Network
##############################################

resource "google_compute_network" "prodsmall" {
  name       = "${var.network}"
  ipv4_range = "10.0.0.0/16"
} 

resource "google_compute_firewall" "prodsmall_web" {
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

resource "google_compute_firewall" "prodsmall_ssh" {
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

resource "google_compute_firewall" "prodsmall_internal" {
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
    default = "base-1462485956"
}

module "prodsmall_vpn" {
    source = "./gce/vpn"
    image = "${var.vpn_image}"
    network = "${google_compute_network.prodsmall.name}"
}

##############################################
#Consul Cluster
##############################################

variable "consul_cluser_image" { 
    default = "consul-server-1461715686"
}

module "prodsmall_consul_cluster" {
    source = "./gce/consul"
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

variable "kakfa_image" { 
    default = "prod-small-kafka-1462989188"
}

variable "db_image" { 
    default = "prod-small-db-1462770003"
}

variable "es_image" { 
    default = "prod-small-es-1463165866"
}

variable "phoenix_image" { 
    default = "prod-small-phoenix-1463379225"
}

variable "greenriver_image" { 
    default = "prod-small-green-1462907498"
}

variable "front_image" { 
    default = "prod-small-front-1463162013"
}

module "prodsmall_stack" {
    source = "./gce/prodsmall_stack"
    zone = "${var.zone}"
    network = "${var.network}"
    datacenter = "${var.network}"
    kafka_image = "${var.kakfa_image}"
    db_image = "${var.db_image}"
    es_image = "${var.es_image}"
    phoenix_image = "${var.phoenix_image}"
    greenriver_image = "${var.greenriver_image}"
    front_image = "${var.front_image}"
    consul_leader = "${module.prodsmall_consul_cluster.leader}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
