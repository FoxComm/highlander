variable "ssh_user" {}
variable "ssh_private_key" {}
variable "aws_access_key" {}
variable "aws_secret_key" {}
variable "aws_key_name" {}
variable "region" {}
variable "public_subnet_cidr" {}
variable "private_subnet_cidr" {}
variable "stage_amigo_server_image" {}
variable "stage_backend_image" {}
variable "stage_frontend_image" {}

variable "vpn_image" {}

provider "aws" {
    access_key = "${var.aws_access_key}"
    secret_key = "${var.aws_secret_key}"
    region = "${var.region}"
}

module "target_networking" {
    source = "../../modules/aws/networking"
    vpc_cidr = "10.0.0.0/16"
    public_subnet_cidr = "${var.public_subnet_cidr}"
    private_subnet_cidr = "${var.private_subnet_cidr}"
    name = "tgt"
    key_name = "${var.aws_key_name}"
    vpn_image = "${var.vpn_image}"
}

module "target_stage1" {
    source = "../../modules/aws/tinyprod"
    key_name = "${var.aws_key_name}"
    web_access_sg = "${module.target_networking.web_access_from_nat_sg_id}"
    vpn_access_sg = "${module.target_networking.access_from_vpn_sg}"
    subnet = "${module.target_networking.private_subnet_id}"
    datacenter = "stage1"
    amigo_server_image = "${var.stage_amigo_server_image}"
    backend_image = "${var.stage_backend_image}"
    frontend_image ="${var.stage_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
