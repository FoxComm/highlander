variable "key_name" {}
variable "image" {}
variable "datacenter" {}
variable "ssh_user" {} 
variable "ssh_private_key" {} 

variable "subnet_id" {} 
variable "security_groups" {
	type = "list"
} 

resource "aws_instance" "amigo_server_0" {
	ami = "${var.image}"
	instance_type = "t2.medium"
	key_name = "${var.key_name}"

	tags = {
		Name = "${var.datacenter}-amigo-server-0"
		Datacenter = "${var.datacenter}"
	}

	subnet_id = "${var.subnet_id}"
	vpc_security_group_ids = "${var.security_groups}"
	availability_zone = "us-west-2a"
	associate_public_ip_address = false	

	root_block_device {
		volume_type = "standard"
		volume_size = "30"
	}

    # service_account {
    #     scopes = ["storage-rw"]
    # }

    connection { 
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap.sh",
          "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${aws_instance.amigo_server_0.private_ip}",
          "sudo su -c 'echo 1 > /var/lib/zookeeper/myid'"
        ]
    }
}

resource "aws_instance" "amigo_server_1" {
	ami = "${var.image}"
	instance_type = "t2.medium"
	key_name = "${var.key_name}"

	tags = {
		Name = "${var.datacenter}-amigo-server-1"
		Datacenter = "${var.datacenter}"
	}

	subnet_id = "${var.subnet_id}"
	vpc_security_group_ids = "${var.security_groups}"
	availability_zone = "us-west-2a"
	associate_public_ip_address = false	

	root_block_device {
		volume_type = "standard"
		volume_size = "30"
	}

    # service_account {
    #     scopes = ["storage-rw"]
    # }

    connection { 
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap.sh",
          "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${aws_instance.amigo_server_0.private_ip}",
          "sudo su -c 'echo 2 > /var/lib/zookeeper/myid'"
        ]
    }
}

resource "aws_instance" "amigo_server_2" {
	ami = "${var.image}"
	instance_type = "t2.medium"
	key_name = "${var.key_name}"

	tags = {
		Name = "${var.datacenter}-amigo-server-2"
		Datacenter = "${var.datacenter}"
	}

	subnet_id = "${var.subnet_id}"
	vpc_security_group_ids = "${var.security_groups}"
	availability_zone = "us-west-2a"
	associate_public_ip_address = false	

	root_block_device {
		volume_type = "standard"
		volume_size = "30"
	}

    # service_account {
    #     scopes = ["storage-rw"]
    # }

    connection { 
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap.sh",
          "/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${aws_instance.amigo_server_0.private_ip}",
          "sudo su -c 'echo 3 > /var/lib/zookeeper/myid'"
        ]
    }
}