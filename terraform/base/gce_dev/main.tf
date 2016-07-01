variable "ssh_user" {} 
variable "ssh_private_key" {}

variable "account_file" {}
variable "gce_project" {}
variable "region" {}
variable "consul_cluster_image" {}
variable "tiny_backend_image" {} 
variable "tiny_frontend_image" {} 
variable "consul_server_image" {}
variable "gatling_image" {}

provider "google" 
{
    credentials = "${file(var.account_file)}"
    project = "${var.gce_project}"
    region = "${var.region}"
}

##############################################
# Setup consul cluster
##############################################
module "consul_cluster" {
    source = "../../modules/gce/consul"
    datacenter = "dev"
    network = "default"
    servers = 3
    image = "${var.consul_cluster_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

##############################################
# Setup buildkite build machines
##############################################
module "buildagents" {
    source = "../../modules/gce/agent"
    prefix = "buildkite-agent"
    queue = "core"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    servers = 4
}

##############################################
# Setup Gatling Machines
##############################################
module "gatling" {
    source = "../../modules/gce/tinystack"
    datacenter = "gatling"
    backend_image = "${var.tiny_backend_image}"
    frontend_image = "${var.tiny_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${module.consul_cluster.leader}"
    consul_server_image = "${var.consul_server_image}"
}

resource "google_compute_instance" "gatling-gun"{ 
    name = "gatling-gun"
    machine_type = "n1-standard-4"
    tags = ["no-ip", "gatling-gun"]
    zone = "us-central1-a"

    disk {
        image = "${var.gatling_image}"
        type = "pd-ssd"
        size = "30"
    }   

    network_interface {
        network = "default"
    }

    connection { 
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "file" {
        source = "terraform/scripts/bootstrap.sh"
        destination = "/tmp/bootstrap.sh"
    }

    provisioner "file" {
        source = "terraform/scripts/consul.sh"
        destination = "/tmp/consul.sh"
    }

    provisioner "remote-exec" {
        inline = [
          "chmod +x /tmp/bootstrap.sh",
          "chmod +x /tmp/consul.sh",
          "/tmp/bootstrap.sh",
          "/tmp/consul.sh gatling ${module.gatling.consul_address}"
        ]
    }
}

##############################################
# Setup Stage
##############################################
module "stagem" {
    source = "../../modules/gce/tinystack"
    datacenter = "stage"
    backend_image = "${var.tiny_backend_image}"
    frontend_image = "${var.tiny_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${module.consul_cluster.leader}"
    consul_server_image = "${var.consul_server_image}"
}

##############################################
# Setup Dem 1 & 2
##############################################
module "dem1" {
    source = "../../modules/gce/demostack"
    prefix = "dem1"
    ssh_user = "${var.ssh_user}"
    backend_image = "ubuntu-1604-xenial-v20160610"
    frontend_image = "ubuntu-1604-xenial-v20160610"
    ssh_private_key = "${var.ssh_private_key}"
}

module "dem2" {
    source = "../../modules/gce/demostack"
    prefix = "dem2"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

##############################################
# Setup Usertest 1 & 2
##############################################
module "usertest1" {
    source = "../../modules/gce/twostack"
    prefix = "usertest1"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "usertest2" {
    source = "../../modules/gce/twostack"
    prefix = "usertest2"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
