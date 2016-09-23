variable "key_name" {}
variable "datacenter" {}
variable "ssh_user" {}
variable "ssh_private_key" {}
variable "subnet_id" {}
variable "security_groups" {
	type = "list"
}

variable "kafka_image" {}
variable "db_image" {}
variable "es_image" {}
variable "log_image" {}
variable "phoenix_image" {}
variable "greenriver_image" {}
variable "front_image" {}
variable "service_worker_image" {}

variable "amigo_leader" {}

resource "aws_instance" "kafka" {
	ami = "${var.kafka_image}"
	instance_type = "m4.large"
	key_name = "${var.key_name}"

	tags = {
		Name = "target-kafka"
    Datacenter = "${var.datacenter}"
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
        "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${var.amigo_leader}",
      ]
  }
}

resource "aws_instance" "db" {
	ami = "${var.db_image}"
	instance_type = "r3.xlarge"
	key_name = "${var.key_name}"

	tags = {
		Name = "target-db"
    Datacenter = "${var.datacenter}"
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
        "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${var.amigo_leader}",
      ]
  }
}

resource "aws_instance" "es" {
	ami = "${var.es_image}"
	instance_type = "r3.large"
	key_name = "${var.key_name}"

	tags = {
		Name = "target-es"
    Datacenter = "${var.datacenter}"
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
        "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${var.amigo_leader}",
      ]
  }
}

resource "aws_instance" "es-log" {
	ami = "${var.log_image}"
	instance_type = "r3.large"
	key_name = "${var.key_name}"

	tags = {
		Name = "target-es-log"
    Datacenter = "${var.datacenter}"
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
        "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${var.amigo_leader}",
      ]
  }
}

resource "aws_instance" "phoenix" {
	ami = "${var.phoenix_image}"
	instance_type = "m4.xlarge"
	key_name = "${var.key_name}"

	tags = {
		Name = "target-phoenix"
    Datacenter = "${var.datacenter}"
	}

	subnet_id = "${var.subnet_id}"
	vpc_security_group_ids = "${var.security_groups}"
	availability_zone = "us-west-2a"
	associate_public_ip_address = false

	root_block_device {
		volume_type = "standard"
		volume_size = "10"
	}

  connection {
      type = "ssh"
      user = "${var.ssh_user}"
      private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
      inline = [
        "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${var.amigo_leader}",
      ]
  }
}

resource "aws_instance" "greenriver" {
	ami = "${var.greenriver_image}"
	instance_type = "m4.large"
	key_name = "${var.key_name}"

	tags = {
		Name = "target-greenriver"
    Datacenter = "${var.datacenter}"
	}

	subnet_id = "${var.subnet_id}"
	vpc_security_group_ids = "${var.security_groups}"
	availability_zone = "us-west-2a"
	associate_public_ip_address = false

	root_block_device {
		volume_type = "standard"
		volume_size = "10"
	}

  connection {
      type = "ssh"
      user = "${var.ssh_user}"
      private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
      inline = [
        "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${var.amigo_leader}",
      ]
  }
}

resource "aws_instance" "service-worker-0" {
	ami = "${var.service_worker_image}"
	instance_type = "m4.xlarge"
	key_name = "${var.key_name}"

	tags = {
		Name = "target-service-worker-0"
    Datacenter = "${var.datacenter}"
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
        "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${var.amigo_leader}",
      ]
  }
}

/*resource "aws_instance" "front-worker-0" {
	ami = "${var.front_image}"
	instance_type = "m4.large"
	key_name = "${var.key_name}"

	tags = {
		Name = "target-front-worker-0"
    Datacenter = "${var.datacenter}"
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
        "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${var.amigo_leader}",
      ]
  }
}*/
