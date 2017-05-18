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
# Setup Apple Pay Feature Branch
##############################################
module "styleguide" {
  source           = "../../modules/gce/appliance"
  instance_name    = "feature-branch-styleguide"
  dns_record       = "feature-branch-styleguide"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}
