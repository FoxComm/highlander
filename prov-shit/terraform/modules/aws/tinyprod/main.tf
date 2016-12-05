variable "key_name" {}
variable "web_access_sg" {}
variable "vpn_access_sg" {}
variable "datacenter" {}
variable "amigo_server_image" {}
variable "backend_image" {}
variable "frontend_image" {}
variable "ssh_user" {}
variable "ssh_private_key" {}
variable "subnet" {}

resource "aws_instance" "tinyprod-amigo" {

    ami = "${var.amigo_server_image}"
    instance_type = "t2.medium"

    tags = {
        Name = "${var.datacenter}-amigo"
        DC = "${var.datacenter}"
        Owner = "fox"
    }

    subnet_id = "${var.subnet}"
    associate_public_ip_address = false
    vpc_security_group_ids = ["${var.web_access_sg}", "${var.vpn_access_sg}"]
    key_name = "${var.key_name}"
    root_block_device {
        volume_size = "40"
    }

    connection {
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap.sh",
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${aws_instance.tinyprod-amigo.private_ip}",
          "sudo rm -rf /var/consul/* && sudo systemctl restart consul_server.service",
        ]
    }
}

resource "aws_instance" "tinyprod-frontend" {

    ami = "${var.frontend_image}"
    instance_type = "m4.xlarge"

    tags = {
        Name = "${var.datacenter}-frontend"
        DC = "${var.datacenter}"
        Owner = "fox"
    }

    subnet_id = "${var.subnet}"
    associate_public_ip_address = false
    vpc_security_group_ids = ["${var.web_access_sg}", "${var.vpn_access_sg}"]
    key_name = "${var.key_name}"
    root_block_device {
        volume_size = "100"
    }

    connection {
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap.sh",
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${aws_instance.tinyprod-amigo.private_ip}",
        ]
    }
}

resource "aws_instance" "tinyprod-backend" {

    ami = "${var.backend_image}"
    instance_type = "m4.2xlarge"

    tags = {
        Name = "${var.datacenter}-backend"
        DC = "${var.datacenter}"
        Owner = "fox"
    }

    subnet_id = "${var.subnet}"
    associate_public_ip_address = false
    vpc_security_group_ids = ["${var.web_access_sg}", "${var.vpn_access_sg}"]
    key_name = "${var.key_name}"

    root_block_device {
        volume_size = "500"
    }

    connection {
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap.sh",
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${aws_instance.tinyprod-amigo.private_ip}",
        ]
    }
}
