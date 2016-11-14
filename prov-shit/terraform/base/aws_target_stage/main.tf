# provider variables
variable "access_key" {
}
variable "secret_key" {
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
variable "key_name" {
}
variable "policy_file" {
}

# network variables
variable "vpc_cidr_block" {
}

# resources variables
variable "amigo_image" {
}
variable "amigo_machine_type" {
}
variable "frontend_image" {
}
variable "frontend_machine_type" {
}
variable "backend_image" {
}
variable "backend_machine_type" {
}

# provisioner variables
variable "ssh_user" {
}
variable "ssh_private_key" {
}

provider "aws" {
    access_key = "${var.access_key}"
    secret_key = "${var.secret_key}"
    region     = "${var.region}"
}

module "network" {
    source         = "../../modules/aws/net"
    // generic variables
    datacenter     = "${var.datacenter}"
    setup          = "${var.setup}"
    vpc_cidr_block = "${var.vpc_cidr_block}"
}

resource "aws_s3_bucket" "docker_registry_bucket" {
    bucket = "${var.datacenter}-${var.setup}-docker"
    acl    = "private"

    tags {
        Name        = "${var.datacenter}-${var.setup}-docker"
        Datacenter  = "${var.datacenter}"
        Environment = "${var.setup}"
    }

    policy = "${file(var.policy_file)}"
}

//module "amigo" {
//    source          = "../../modules/aws/server"
//    // generic variables
//    zone            = "${var.zone}"
//    datacenter      = "${var.datacenter}"
//    setup           = "${var.setup}"
//    network         = "${var.network}"
//    // resources variables
//    image           = "${var.amigo_image}"
//    machine_type    = "${var.amigo_machine_type}"
//    machine_role    = "amigo-server"
//    disk_size       = "${var.amigo_disk_size}"
//    count           = 1
//    key_name        = "${var.key_name}"
//    security_groups = "${var.amigo_security_groups}"
//    // provisioner variables
//    ssh_user        = "${var.ssh_user}"
//    ssh_private_key = "${var.ssh_private_key}"
//}

