variable "ssh_user" {}
variable "ssh_private_key" {}

variable "dnsimple_token" {}
variable "dnsimple_email" {}

variable "account_file" {}
variable "gce_project" {}
variable "region" {}

variable "network" {}
variable "zone" {}

variable "nano_amigo_image" {}
variable "nano_storage_image" {}
variable "nano_worker_image" {}

provider "google" {
    credentials = "${file(var.account_file)}"
    project = "${var.gce_project}"
    region = "${var.region}"
}

provider "dnsimple" {
    token = "${var.dnsimple_token}"
    email = "${var.dnsimple_email}"
}

##############################################
# Storage Buckets
##############################################
resource "google_storage_bucket" "database-backups" {
  name     = "tpg-nano-db-backups"
  location = "us"
}

resource "google_storage_bucket" "docker-registry" {
  name     = "tpg-nano-docker"
  location = "us"
}

##############################################
# Staging
##############################################
module "tpg_nano_staging" {
    source = "../../modules/gce/nanostack"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"

    nano_amigo_image = "${var.nano_amigo_image}"
    nano_storage_image = "${var.nano_storage_image}"
    nano_worker_image = "${var.nano_worker_image}"

    network = "${var.network}"
    zone = "${var.zone}"
    datacenter = "tpg-nano-stage"
    subdomain = "tpg-stage"
    join_type = "join"
}

##############################################
# Production
##############################################
module "tpg_nano_production" {
    source = "../../modules/gce/nanostack"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"

    nano_amigo_image = "${var.nano_amigo_image}"
    nano_storage_image = "${var.nano_storage_image}"
    nano_worker_image = "${var.nano_worker_image}"

    network = "${var.network}"
    zone = "${var.zone}"
    datacenter = "tpg-nano-prod"
    subdomain = "tpg"
    join_type = "join"
}
