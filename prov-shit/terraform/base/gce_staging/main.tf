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

##############################################
# Perfect Gourmet Setup Staging
##############################################
module "perfect-gourmet-staging" {
    source = "../../modules/gce/tinystack"
    datacenter = "perfect-gourmet-stage"
    backend_image = "${var.tiny_backend_image}"
    frontend_image = "${var.tiny_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${var.consul_leader}"
    consul_server_image = "${var.consul_server_image}"
}

##############################################
# Top Drawer Setup Staging
##############################################
module "top-drawer-staging" {
    source = "../../modules/gce/tinystack"
    datacenter = "top-drawer-stage"
    backend_image = "${var.tiny_backend_image}"
    frontend_image = "${var.tiny_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${var.consul_leader}"
    consul_server_image = "${var.consul_server_image}"
}
