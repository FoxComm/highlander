variable "aws_access_key" {}
variable "aws_secret_key" {}
variable "aws_key_name" {}

provider "aws" {
    access_key = "${var.aws_access_key}"
    secret_key = "${var.aws_secret_key}"
    region = "us-west-2"
}

module "demo_networking" {
    source ="./aws/networking"
    vpc_cidr = "10.0.0.0/16"
    public_subnet_cidr = "10.0.0.0/24"
    private_subnet_cidr = "10.0.1.0/24"
    name = "demo"
    key_name = "${var.aws_key_name}"
}


