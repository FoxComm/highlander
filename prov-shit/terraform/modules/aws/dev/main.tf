variable aws_ssh_user {}

variable aws_ssh_private_key {}

variable key_name {}

variable datacenter {}

variable subnet_id {}

variable security_groups {
  type = "list"
}

variable image {}

variable instance_type {}

variable volume_size {}

resource "aws_instance" "amigo_leader" {
  ami                    = "${var.image}"
  instance_type          = "${var.instance_type}"
  key_name               = "${var.key_name}"
  subnet_id              = "${var.subnet_id}"
  vpc_security_group_ids = "${var.security_groups}"

  associate_public_ip_address = false

  root_block_device {
    volume_type = "standard"
    volume_size = "${var.volume_size}"
  }

  tags = {
    Name = "${var.datacenter}-amigo-leader"
  }

  connection {
    type        = "ssh"
    user        = "${var.aws_ssh_user}"
    private_key = "${file(var.aws_ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${aws_instance.amigo_leader.0.private_ip}",
      "sudo su -c 'echo 1 > /var/lib/zookeeper/myid'",
      "sudo rm -rf /var/consul/* && sudo systemctl restart consul_server.service",
    ]
  }
}
