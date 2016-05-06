
variable "ssh_user" {} 
variable "ssh_private_key" {} 

provider "google" 
{
    credentials = "${file("account.json")}"
    project = "foxcomm-staging"
    region = "us-central1"
}

##############################################
#Setup consul cluster
##############################################

variable "consul_cluser_image" { 
    default = "consul-server-1461715686"
}

module "consul_cluster" {
    source = "./gce/consul"
    datacenter = "dev"
    network = "default"
    servers = 3
    image = "${var.consul_cluser_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

##############################################
#Setup buildkite build machines
##############################################

module "buildagents" {
    source = "./gce/agent"
    prefix = "buildkite-agent"
    queue = "core"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    servers = 4
}

##############################################
#Setup Gatling Machines
##############################################

variable "tiny_backend_image" {
    default = "tinystack-backend-1461799315"
} 

variable "tiny_frontend_image" {
    default = "tinystack-frontend-1461787500"
} 

variable "consul_server_image" { 
    default = "tinystack-consul-server-1461881696"
}

module "gatling" {
    source = "./gce/tinystack"
    datacenter = "gatling"
    backend_image = "${var.tiny_backend_image}"
    frontend_image = "${var.tiny_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${module.consul_cluster.leader}"
    consul_server_image = "${var.consul_server_image}"
}

variable "gatling_image" { 
    default = "base-jvm-1461863900"
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
        private_key="${file(var.ssh_private_key)}"
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
#Setup Stage
##############################################

module "stagem" {
    source = "./gce/tinystack"
    datacenter = "stage"
    backend_image = "${var.tiny_backend_image}"
    frontend_image = "${var.tiny_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${module.consul_cluster.leader}"
    consul_server_image = "${var.consul_server_image}"
}

##############################################
#Setup Dem 1 & 2
##############################################

module "dem1" {
    source = "./gce/demostack"
    prefix = "dem1"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "dem2" {
    source = "./gce/demostack"
    prefix = "dem2"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

##############################################
#Setup Usertest 1 & 2
##############################################

module "usertest1" {
    source = "./gce/twostack"
    prefix = "usertest1"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "usertest2" {
    source = "./gce/twostack"
    prefix = "usertest2"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
