variable "aws_access_key" {}

variable "aws_secret_key" {}

variable "aws_ssh_user" {}

variable "aws_ssh_private_key" {}

variable "key_name" {}

variable "region" {}

variable "subnet_id" {}

variable "amigo_image" {}

variable "amigo_instance_type" {}

variable "amigo_leader_datacenter" {}

variable "amigo_leader_image" {}

variable "amigo_security_groups" {
  type = "list"
}

variable "amigo_subnet_id" {}

variable "amigo_volume_size" {}

provider "aws" {
  access_key = "${var.aws_access_key}"
  secret_key = "${var.aws_secret_key}"
  region     = "${var.region}"
}

resource "aws_instance" "amigo_leader" {
  ami                    = "${var.amigo_leader_image}"
  instance_type          = "${var.amigo_instance_type}"
  key_name               = "${var.key_name}"
  subnet_id              = "${var.amigo_subnet_id}"
  vpc_security_group_ids = "${var.amigo_security_groups}"

  associate_public_ip_address = false

  root_block_device {
    volume_type = "standard"
    volume_size = "${var.amigo_volume_size}"
  }

  tags = {
    Name = "${var.amigo_leader_datacenter}-amigo-leader"
  }

  connection {
    type        = "ssh"
    user        = "${var.aws_ssh_user}"
    private_key = "${file(var.aws_ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap_consul_aws.sh ${var.amigo_leader_datacenter} ${aws_instance.amigo_leader.0.private_ip}",
      "sudo su -c 'echo 1 > /var/lib/zookeeper/myid'",
      "sudo rm -rf /var/consul/* && sudo systemctl restart consul_server.service"
    ]
  }
}

module "tgt-wild" {
  source = "../../modules/aws/tinystack"
  aws_ssh_user = "${var.aws_ssh_user}"
  aws_ssh_private_key = "${var.aws_ssh_private_key}"
  consul_leader = "${aws_instance.amigo_leader.0.private_ip}"
  datacenter = "tgt-wild"
  key_name = "${var.key_name}"
  subnet_id = "${var.amigo_subnet_id}"

  amigo_image = "${var.amigo_image}"
  amigo_instance_type = "${var.amigo_instance_type}"
  amigo_security_groups = "${var.amigo_security_groups}"
  amigo_volume_size = "${var.amigo_volume_size}"
}
