variable "ssh_user" {}
variable "ssh_private_key" {}

variable "account_file" {}
variable "gce_project" {}
variable "region" {}
variable "base_image" {}
variable "tiny_backend_image" {}
variable "tiny_frontend_image" {}
variable "consul_server_image" {}
variable "gatling_image" {}
variable "consul_leader" {}

provider "google"
{
    credentials = "${file(var.account_file)}"
    project = "${var.gce_project}"
    region = "${var.region}"
}

##############################################
# Setup Gatling Machines
##############################################
# module "foxcomm-gatling" {
#     source = "../../modules/gce/tinystack"
#     datacenter = "foxcomm-gatling"
#     backend_image = "${var.tiny_backend_image}"
#     frontend_image = "${var.tiny_frontend_image}"
#     ssh_user = "${var.ssh_user}"
#     ssh_private_key = "${var.ssh_private_key}"
#     consul_leader = "${module.consul_cluster.leader}"
#     consul_server_image = "${var.consul_server_image}"
# }

# resource "google_compute_instance" "foxcomm-gatling-gun" {
#     name = "foxcomm-gatling-gun"
#     machine_type = "n1-standard-2"
#     tags = ["no-ip", "foxcomm-gatling-gun"]
#     zone = "us-central1-a"

#     disk {
#         image = "${var.gatling_image}"
#         type = "pd-ssd"
#         size = "30"
#     }

#     network_interface {
#         network = "default"
#     }

#     connection {
#         type = "ssh"
#         user = "${var.ssh_user}"
#         private_key = "${file(var.ssh_private_key)}"
#     }

#     provisioner "remote-exec" {
#         inline = [
#           "/usr/local/bin/bootstrap.sh",
#           "/usr/local/bin/bootstrap_consul.sh foxcomm-gatling ${module.foxcomm-gatling.consul_address}"
#         ]
#     }
# }

##############################################
# Setup Staging
##############################################
module "foxcomm-staging" {
    source = "../../modules/gce/tinystack"
    datacenter = "foxcomm-stage"
    backend_image = "${var.tiny_backend_image}"
    frontend_image = "${var.tiny_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${var.consul_leader}"
    consul_server_image = "${var.consul_server_image}"
    frontend_machine_type = "n1-highmem-8"
}

##############################################
# Target Setup Staging
##############################################
module "target-staging" {
    source = "../../modules/gce/tinystack"
    datacenter = "target-stage"
    backend_image = "${var.tiny_backend_image}"
    frontend_image = "${var.tiny_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${var.consul_leader}"
    consul_server_image = "${var.consul_server_image}"
}
