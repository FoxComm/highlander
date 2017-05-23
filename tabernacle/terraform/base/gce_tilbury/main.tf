variable "ssh_user" {}

variable "ssh_private_key" {}

variable "account_file" {}

variable "gce_project" {}

variable "region" {}

variable "appliance_image" {}

variable "consul_leader" {}

variable "dnsimple_token" {}

variable "dnsimple_account" {}

provider "google" {
  credentials = "${file(var.account_file)}"
  project     = "${var.gce_project}"
  region      = "${var.region}"
}

##############################################
# Setup Charlotte Tilbury Feature Branch
##############################################
module "tilbury" {
  source           = "../../modules/gce/appliance"
  instance_name    = "tilbury"
  dns_record       = "tilbury"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}
