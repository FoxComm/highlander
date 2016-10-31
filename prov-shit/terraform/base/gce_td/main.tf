variable "ssh_user" {}
variable "ssh_private_key" {}

variable "account_file" {}
variable "gce_project" {}
variable "region" {}
variable "network" {}
variable "bucket_location" {}
variable "zone" {}
variable "vpn_image" {}
variable "amigo_server_image" {}
variable "backend_image" {}
variable "frontend_image" {}

provider "google"
{
    credentials = "${file(var.account_file)}"
    project = "${var.gce_project}"
    region = "${var.region}"
}

##############################################
# Small Production Stack
##############################################
module "td-prod" {
    source = "../../modules/gce/tinyprod"
    network = "${var.network}"
    datacenter = "td-prod"
    backend_image = "${var.backend_image}"
    frontend_image = "${var.frontend_image}"
    amigo_server_image = "${var.amigo_server_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
