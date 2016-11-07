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
variable "network" {
}
variable "bucket_location" {
}

# resources variables
variable "machine_type" {
}
variable "image" {
}
variable "disk_size" {
}

# user variables
variable "owner" {
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

module "dev" {
    source          = "../../modules/gce/swarm/server"
    // generic variables
    zone            = "${var.zone}"
    datacenter      = "${var.datacenter}"
    setup           = "dev"
    network         = "${var.network}"
    // resources variables
    machine_role    = "dev-server"
    machine_type    = "${var.machine_type}"
    image           = "${var.image}"
    disk_size       = "${var.disk_size}"
    count           = 1
    // user variables
    owner           = "${var.owner}"
    // provisioner variables
    ssh_user        = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "dev_provision" {
    source       = "../../modules/gce/swarm/dev_provision"
    // resources variables
    host_address = "${element(module.dev.ips, 0)}"
    // user variables
    owner        = "${var.owner}"
}
