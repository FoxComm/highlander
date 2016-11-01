# provisioner variables
variable "ssh_user" {
}
variable "ssh_private_key" {
}

# provider variables
variable "account_file" {
}
variable "gce_project" {
}
variable "region" {
}
variable "zone" {
}
variable "datacenter" {
}
variable "network" {
}
variable "bucket_location" {
}

# image variables
variable "master_image" {
}
variable "masters_count" {
}
variable "worker_image" {
}
variable "workers_count" {
}

provider "google" {
    credentials = "${file(var.account_file)}"
    project     = "${var.gce_project}"
    region      = "${var.region}"
}

resource "google_storage_bucket" "docker-registry" {
    name     = "${var.datacenter}-docker"
    location = "${var.bucket_location}"
}

module "master_cluster" {
    source          = "../../modules/gce/swarm/master"
    zone            = "${var.zone}"
    datacenter      = "${var.datacenter}"
    network         = "${var.network}"
    image           = "${var.master_image}"
    count           = "${var.masters_count}"
    ssh_user        = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "worker_cluster" {
    source          = "../../modules/gce/swarm/worker"
    zone            = "${var.zone}"
    datacenter      = "${var.datacenter}"
    network         = "${var.network}"
    image           = "${var.worker_image}"
    count           = "${var.workers_count}"
    ssh_user        = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

