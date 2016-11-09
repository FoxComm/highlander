# provider variables
variable "account_file" {
}
variable "gce_project" {
}
variable "region" {
}

# generic variables
variable "zone" {
}
variable "datacenter" {
}
variable "setup" {
}
variable "network" {
}
variable "bucket_location" {
}

# resources variables
variable "master_machine_type" {
}
variable "master_image" {
}
variable "master_disk_size" {
}
variable "masters_count" {
}
variable "worker_machine_type" {
}
variable "worker_image" {
}
variable "worker_disk_size" {
}
variable "workers_count" {
}

# provisioner variables
variable "ssh_user" {
}
variable "ssh_private_key" {
}

provider "google" {
    credentials = "${file(var.account_file)}"
    project     = "${var.gce_project}"
    region      = "${var.region}"
}

resource "google_storage_bucket" "docker-registry" {
    name     = "${var.datacenter}-${var.setup}-docker"
    location = "${var.bucket_location}"
}

module "master_cluster" {
    source          = "../../modules/gce/swarm/server"
    // generic variables
    zone            = "${var.zone}"
    datacenter      = "${var.datacenter}"
    setup           = "${var.setup}"
    network         = "${var.network}"
    // resources variables
    machine_role    = "master-server"
    machine_type    = "${var.master_machine_type}"
    image           = "${var.master_image}"
    disk_size       = "${var.master_disk_size}"
    count           = "${var.masters_count}"
    // provisioner variables
    ssh_user        = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "master_cluster_provision" {
    source       = "../../modules/gce/swarm/master_provision"
    # provider variables
    account_file = "${var.account_file}"
    // generic variables
    datacenter   = "${var.datacenter}"
    setup        = "${var.setup}"
    // resources variables
    masters_ips  = "${module.master_cluster.ips}"
    leader_ip    = "${module.master_cluster.leader_ip}"
    count        = "${var.masters_count}"
}

module "worker_cluster" {
    source          = "../../modules/gce/swarm/server"
    // generic variables
    zone            = "${var.zone}"
    datacenter      = "${var.datacenter}"
    setup           = "${var.setup}"
    network         = "${var.network}"
    // resources variables
    machine_role    = "worker-server"
    machine_type    = "${var.worker_machine_type}"
    image           = "${var.worker_image}"
    disk_size       = "${var.worker_disk_size}"
    count           = "${var.workers_count}"
    // provisioner variables
    ssh_user        = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "worker_cluster_provision" {
    source                 = "../../modules/gce/swarm/worker_provision"
    // generic variables
    datacenter             = "${var.datacenter}"
    setup                  = "${var.setup}"
    // resources variables
    masters_ips            = "${module.master_cluster.ips}"
    leader_ip              = "${module.master_cluster.leader_ip}"
    worker_ips             = "${module.worker_cluster.ips}"
    docker_registry_bucket = "${var.datacenter}-${var.setup}-docker"
    count                  = "${var.workers_count}"
}
