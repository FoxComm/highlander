variable "key_name" {}
variable "stage_datacenter" {}

variable "ssh_user" {}
variable "ssh_private_key" {}

variable "subnet_id" {}
variable "security_groups" {
	type = "list"
}
variable "sg_https" {
  type = "list"
}

variable "stage_amigo_image" {}
variable "stage_backend_image" {}
variable "stage_frontend_image" {}

variable "amigo_leader" {}

variable "policy_file" {}

resource "aws_s3_bucket" "s3_docker_stage" {
    bucket = "s3-docker-stage"
    acl = "private"

    tags {
        Name = "Stage Docker Registry"
        Environment = "stage"
    }

  	policy = "${file(var.policy_file)}"
}

resource "aws_instance" "stage_amigo" {
	ami = "${var.stage_amigo_image}"
	instance_type = "t2.medium"
	key_name = "${var.key_name}"

	tags = {
		Name = "stage-amigo-server"
	}

	subnet_id = "${var.subnet_id}"
	vpc_security_group_ids = "${var.security_groups}"
	availability_zone = "us-west-2a"
	associate_public_ip_address = false

	root_block_device {
		volume_type = "standard"
		volume_size = "20"
	}

  connection {
      type = "ssh"
      user = "${var.ssh_user}"
      private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
      inline = [
        "/usr/local/bin/bootstrap_consul_aws.sh ${var.stage_datacenter} ${var.amigo_leader}",
        "sudo su -c 'echo 1 > /var/lib/zookeeper/myid'"
      ]
  }
}

resource "aws_instance" "stage_backend" {
	ami = "${var.stage_backend_image}"
	instance_type = "r3.xlarge"
	key_name = "${var.key_name}"

	tags = {
		Name = "stage-backend"
	}

	subnet_id = "${var.subnet_id}"
	vpc_security_group_ids = "${var.security_groups}"
	availability_zone = "us-west-2a"
	associate_public_ip_address = false

	root_block_device {
		volume_type = "standard"
		volume_size = "100"
	}

  connection {
      type = "ssh"
      user = "${var.ssh_user}"
      private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
      inline = [
        "/usr/local/bin/bootstrap_consul_aws.sh ${var.stage_datacenter} ${aws_instance.stage_amigo.private_ip}",
      ]
  }
}

resource "aws_instance" "stage_frontend" {
	ami = "${var.stage_frontend_image}"
	instance_type = "m4.xlarge"
	key_name = "${var.key_name}"

	tags = {
		Name = "stage-frontend"
	}

	subnet_id = "${var.subnet_id}"
	vpc_security_group_ids = "${var.sg_https}"
	availability_zone = "us-west-2a"
	associate_public_ip_address = false

	root_block_device {
		volume_type = "standard"
		volume_size = "30"
	}

  connection {
      type = "ssh"
      user = "${var.ssh_user}"
      private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
      inline = [
        "/usr/local/bin/bootstrap_consul_aws.sh ${var.stage_datacenter} ${aws_instance.stage_amigo.private_ip}",
      ]
  }
}
