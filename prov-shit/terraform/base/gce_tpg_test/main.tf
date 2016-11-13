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
module "perfect-gourmet-test" {
    source = "../../modules/gce/tinystack"
    datacenter = "perfect-gourmet-test"
    backend_image = "${var.tiny_backend_image}"
    frontend_image = "${var.tiny_frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${var.consul_leader}"
    consul_server_image = "${var.consul_server_image}"
    frontend_machine_type = "n1-highmem-8"
}
