variable "aws_access_key" {}
variable "aws_secret_key" {}
variable "aws_key_name" {}
variable "region" {}
variable "public_subnet_cidr" {}
variable "private_subnet_cidr" {}

provider "aws" {
    access_key = "${var.aws_access_key}"
    secret_key = "${var.aws_secret_key}"
    region = "${var.region}"
}

module "demo_networking" {
    source ="../../modules/aws/networking"
    vpc_cidr = "10.0.0.0/16"
    public_subnet_cidr = "${var.public_subnet_cidr}"
    private_subnet_cidr = "${var.private_subnet_cidr}"
    name = "demo"
    key_name = "${var.aws_key_name}"
}
