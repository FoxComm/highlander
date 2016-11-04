# user variables
variable "owner" {
}
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
variable "image" {
}

provider "google" {
    credentials = "${file(var.account_file)}"
    project     = "${var.gce_project}"
    region      = "${var.region}"
}

module "dev" {
    source          = "../../modules/gce/swarm/dev"
    owner           = "${var.owner}"
    zone            = "${var.zone}"
    datacenter      = "${var.datacenter}"
    network         = "${var.network}"
    image           = "${var.image}"
    ssh_user        = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
