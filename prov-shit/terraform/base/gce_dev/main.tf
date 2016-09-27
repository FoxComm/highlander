variable "ssh_user" {}
variable "ssh_private_key" {}

variable "account_file" {}
variable "gce_project" {}
variable "region" {}
variable "base_image" {}
variable "consul_cluster_image" {}
variable "tiny_backend_image" {}
variable "tiny_frontend_image" {}
variable "consul_server_image" {}
variable "gatling_image" {}
variable "builder_image" {}

provider "google"
{
    credentials = "${file(var.account_file)}"
    project = "${var.gce_project}"
    region = "${var.region}"
}

##############################################
# Setup Consul Cluster
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
# Setup Buildkite Agent Machines
##############################################
module "buildagents" {
    source = "../../modules/gce/agent"
    prefix = "buildkite-agent"
    queue = "core"
    image = "${var.builder_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    servers = 8
}

##############################################
# Setup Highlander Gatling Machines
##############################################
module "highlander-gatling" {
    source = "../../modules/gce/tinystack"
    datacenter = "highlander-gatling"
    backend_image = "${var.tiny_backend_image}"
    frontend_image = "${var.tiny_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${module.consul_cluster.leader}"
    consul_server_image = "${var.consul_server_image}"
}

resource "google_compute_instance" "highlander-gatling-gun" {
    name = "highlander-gatling-gun"
    machine_type = "n1-standard-2"
    tags = ["no-ip", "highlander-gatling-gun"]
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

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap.sh",
          "/usr/local/bin/bootstrap_consul.sh highlander-gatling ${module.highlander-gatling.consul_address}"
        ]
    }
}

##############################################
# Setup Highlander Staging
##############################################
module "highlander-staging" {
    source = "../../modules/gce/tinystack"
    datacenter = "highlander-stage"
    backend_image = "${var.tiny_backend_image}"
    frontend_image = "${var.tiny_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${module.consul_cluster.leader}"
    consul_server_image = "${var.consul_server_image}"
}
