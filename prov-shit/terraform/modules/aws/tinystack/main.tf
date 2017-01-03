variable "aws_ssh_user" {}

variable "aws_ssh_private_key" {}

variable "consul_leader" {}

variable "datacenter" {}

variable "dns_entry" {}

variable "amigo_image" {}

variable "amigo_instance_type" {}

variable "amigo_volume_size" {}

variable "backend_image" {}

variable "backend_instance_type" {}

variable "backend_volume_size" {}

variable "frontend_image" {}

variable "frontend_instance_type" {}

variable "frontend_volume_size" {}

variable "key_name" {}

variable "public_subnet_id" {}

variable "private_subnet_id" {}

variable "public_security_groups" {
  type = "list"
}

variable "private_security_groups" {
  type = "list"
}

resource "aws_instance" "tiny_consul" {
  ami                    = "${var.amigo_image}"
  instance_type          = "${var.amigo_instance_type}"
  key_name               = "${var.key_name}"
  subnet_id              = "${var.private_subnet_id}"
  vpc_security_group_ids = "${var.private_security_groups}"

  associate_public_ip_address = false

  root_block_device {
    volume_type = "standard"
    volume_size = "${var.amigo_volume_size}"
  }

  tags = {
    Name = "${var.datacenter}-amigo-server"
  }

  connection {
    type        = "ssh"
    user        = "${var.aws_ssh_user}"
    private_key = "${file(var.aws_ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${var.consul_leader}",
      "sudo su -c 'echo 1 > /var/lib/zookeeper/myid'",
      "sudo rm -rf /var/consul/* && sudo systemctl restart consul_server.service",
    ]
  }
}

resource "aws_instance" "tiny_backend" {
  ami                    = "${var.backend_image}"
  instance_type          = "${var.backend_instance_type}"
  key_name               = "${var.key_name}"
  subnet_id              = "${var.private_subnet_id}"
  vpc_security_group_ids = "${var.private_security_groups}"

  associate_public_ip_address = false

  root_block_device {
    volume_type = "standard"
    volume_size = "${var.backend_volume_size}"
  }

  tags = {
    Name = "${var.datacenter}-backend-server"
  }

  connection {
    type        = "ssh"
    user        = "${var.aws_ssh_user}"
    private_key = "${file(var.aws_ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${aws_instance.tiny_consul.0.private_ip}",
      "sudo su -c 'echo 1 > /var/lib/zookeeper/myid'",
    ]
  }
}

resource "aws_instance" "tiny_frontend" {
  ami                    = "${var.frontend_image}"
  instance_type          = "${var.frontend_instance_type}"
  key_name               = "${var.key_name}"
  subnet_id              = "${var.public_subnet_id}"
  vpc_security_group_ids = "${var.public_security_groups}"

  associate_public_ip_address = true

  root_block_device {
    volume_type = "standard"
    volume_size = "${var.frontend_volume_size}"
  }

  tags = {
    Name = "${var.datacenter}-frontend-server"
  }

  connection {
    type        = "ssh"
    user        = "${var.aws_ssh_user}"
    private_key = "${file(var.aws_ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${aws_instance.tiny_consul.0.private_ip}",
      "sudo su -c 'echo 1 > /var/lib/zookeeper/myid'",
    ]
  }
}

resource "dnsimple_record" "amigo_dns_record" {
  domain = "foxcommerce.com"
  name   = "amigo.${var.dns_entry}"
  value  = "${aws_instance.tiny_consul.0.private_ip}"
  type   = "A"
  ttl    = 3600
}

resource "dnsimple_record" "frontend_dns_record" {
  domain = "foxcommerce.com"
  name   = "${var.dns_entry}"
  value  = "${aws_instance.tiny_frontend.0.public_ip}"
  type   = "A"
  ttl    = 3600
}
