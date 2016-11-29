# provider variables
variable "account_file" {
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
    shared_credentials_file = "${var.account_file}"
    region                  = "${var.region}"
}

module "bastion" {
    source          = "../../modules/aws/server"
    // generic variables
    zone            = "${var.zone}"
    datacenter      = "${var.datacenter}"
    setup           = "${var.setup}"
    network         = "subnet-ba2c3ede"
    // resources variables
    image           = "ami-a9d276c9" # ubuntu 16.04 lts
    machine_type    = "t2.small"
    machine_role    = "bastion-server"
    key_name        = "${var.key_name}"
    security_groups = ["sg-8b0472f2"]
    public_ip       = true
    // provisioner variables
    ssh_user        = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
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

