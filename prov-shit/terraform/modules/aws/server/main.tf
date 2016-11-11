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
variable "security_groups" {
    type = "list"
}

variable "count" {
}
variable "image" {
}
variable "instance_type" {
}
variable "key_name" {
}
variable "machine_role" {
}
variable "public_ip" {
}
variable "disk_size" {
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
        Name = "${join("-", compact(list(var.datacenter, var.setup, var.machine_role, lookup(map("1",""), format("%s", var.count), format("%d", count.index)))))}"
    }

    ami                         = "${var.image}"
    instance_type               = "${var.instance_type}"
    count                       = "${var.count}"

    key_name                    = "${var.key_name}"

    vpc_security_group_ids      = "${var.security_groups}"
    associate_public_ip_address = "${var.public_ip}"

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
