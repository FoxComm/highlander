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
# Setup Coupons Feature Branch
##############################################
module "coupons" {
  source           = "../../modules/gce/appliance"
  instance_name    = "feature-branch-coupons"
  dns_record       = "feature-branch-coupons"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}

##############################################
# Setup Storefront (Peacock) Feature Branch
##############################################
module "peacock" {
  source           = "../../modules/gce/appliance"
  instance_name    = "feature-branch-peacock"
  dns_record       = "feature-branch-peacock"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}

##############################################
# Setup Amazon Feature Branch
##############################################
module "amazon" {
  source           = "../../modules/gce/appliance"
  instance_name    = "feature-branch-amazon"
  dns_record       = "feature-branch-amazon"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}
