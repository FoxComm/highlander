# generic variables
variable "zone" {
}
variable "datacenter" {
}
variable "setup" {
}
variable "network" {
}

# resources variables
variable "image" {
}
variable "machine_type" {
}
variable "machine_role" {
}
variable "disk_size" {
    default = 20
}
variable "count" {
    default = 1
}
variable "key_name" {
}
variable "security_groups" {
    type = "list"
}
variable "public_ip" {
    default = "false"
}

# provisioner variables
variable "ssh_user" {
}
variable "ssh_private_key" {
}

resource "aws_instance" "server" {
    availability_zone           = "${var.zone}"
    subnet_id                   = "${var.network}"

    tags                        = {
        Name = "${var.datacenter}-${var.setup}-${var.machine_role}-${var.count}-${count.index}"
        owner = "fox"
    }

    ami                         = "${var.image}"
    instance_type               = "${var.machine_type}"
    count                       = "${var.count}"

    key_name                    = "${var.key_name}"

    vpc_security_group_ids      = "${var.security_groups}"
    associate_public_ip_address = "${var.public_ip}"

    iam_instance_profile        = "arn:aws:iam::539124908826:instance-profile/fc-stage-instance"

    root_block_device {
        volume_type = "standard"
        volume_size = "${var.disk_size}"
    }

    connection {
        type        = "ssh"
        user        = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }
}
