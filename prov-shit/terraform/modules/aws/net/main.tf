# generic variables
variable "datacenter" {
}
variable "setup" {
}
variable "vpc_cidr_block" {
}

resource "aws_vpc" "default" {
    cidr_block           = "${var.vpc_cidr_block}"
    instance_tenancy     = "default"

    enable_dns_support   = true
    enable_dns_hostnames = true

    tags {
        name = "${var.datacenter}-${var.setup}"
    }
}
