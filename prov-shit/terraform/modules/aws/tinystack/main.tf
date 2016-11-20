variable "aws_ssh_user" {}

variable "aws_ssh_private_key" {}

variable "consul_leader" {}

variable "datacenter" {}

variable "amigo_image" {}

variable "amigo_instance_type" {}

variable "amigo_security_groups" {
  type = "list"
}

variable "amigo_volume_size" {}

variable "key_name" {}

variable "subnet_id" {}

resource "aws_instance" "tiny_consul" {
  ami                    = "${var.amigo_image}"
  instance_type          = "${var.amigo_instance_type}"
  key_name               = "${var.key_name}"
  subnet_id              = "${var.subnet_id}"
  vpc_security_group_ids = "${var.amigo_security_groups}"

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

#resource "aws_instance" "tiny_backend" {
  #ami                    = "${var.amigo_image}"
  #instance_type          = "${var.amigo_instance_type}"
  #key_name               = "${var.key_name}"
  #subnet_id              = "${var.subnet_id}"
  #vpc_security_group_ids = "${var.amigo_security_groups}"

  #associate_public_ip_address = false

  #root_block_device {
    #volume_type = "standard"
    #volume_size = "${var.amigo_volume_size}"
  #}

  #tags = {
    #Name = "${var.datacenter}-amigo-server"
  #}

  #connection {
    #type        = "ssh"
    #user        = "${var.aws_ssh_user}"
    #private_key = "${file(var.aws_ssh_private_key)}"
  #}

  #provisioner "remote-exec" {
    #inline = [
      #"/usr/local/bin/bootstrap_consul_aws.sh ${var.datacenter} ${var.consul_leader}",
      #"sudo su -c 'echo 1 > /var/lib/zookeeper/myid'",
      #"sudo rm -rf /var/consul/* && sudo systemctl restart consul_server.service",
    #]
  #}
#}

#resource "amazon_instance" "tiny-frontend" {


#name = "${var.datacenter}-frontend"


#}


#resource "amazon_instance" "tiny-backend" {


#name = "${var.datacenter}-backend"


#}

