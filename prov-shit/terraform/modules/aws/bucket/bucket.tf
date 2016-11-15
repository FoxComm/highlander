variable "datacenter" {
}
variable "setup" {
}
variable "name" {
}
variable "policy_file" {
}

resource "aws_s3_bucket" "docker_registry_bucket" {
    bucket = "${var.datacenter}-${var.setup}-${var.name}"
    acl    = "private"

    tags {
        Name        = "${var.datacenter}-${var.setup}-${var.name}"
        Datacenter  = "${var.datacenter}"
        Environment = "${var.setup}"
    }

    policy = "${file(var.policy_file)}"
}
