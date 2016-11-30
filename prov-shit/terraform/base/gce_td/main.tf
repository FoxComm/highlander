variable "ssh_user" {}
variable "ssh_private_key" {}

variable "account_file" {}
variable "gce_project" {}
variable "region" {}
variable "network" {}
variable "bucket_location" {}
variable "zone" {}
variable "vpn_image" {}
variable "stage_amigo_server_image" {}
variable "stage_backend_image" {}
variable "stage_frontend_image" {}
variable "prod_amigo_server_image" {}
variable "prod_backend_image" {}
variable "prod_frontend_image" {}

provider "google"
{
    credentials = "${file(var.account_file)}"
    project = "${var.gce_project}"
    region = "${var.region}"
}

##############################################
# We decided td-prod is actually td-stage
##############################################
module "td-prod" {
    source = "../../modules/gce/tinyprod"
    network = "${var.network}"
    datacenter = "td-prod"
    backend_image = "${var.stage_backend_image}"
    frontend_image = "${var.stage_frontend_image}"
    amigo_server_image = "${var.stage_amigo_server_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

##############################################
# Real production
##############################################
module "td-production" {
    source = "../../modules/gce/tinyprod"
    network = "${var.network}"
    datacenter = "td-production"
    backend_image = "${var.prod_backend_image}"
    frontend_image = "${var.prod_frontend_image}"
    amigo_server_image = "${var.prod_amigo_server_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
