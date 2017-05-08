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
module "applepay" {
  source           = "../../modules/gce/appliance"
  instance_name    = "feature-branch-apple"
  dns_record       = "feature-branch-apple"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}

##############################################
# Setup Tumi Instance
##############################################
module "tumi" {
  source           = "../../modules/gce/appliance"
  instance_name    = "tumi"
  dns_record       = "tumi"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}

##############################################
# Setup Tumi2 Instance
##############################################
module "tumi2" {
  source           = "../../modules/gce/appliance"
  instance_name    = "tumi2"
  dns_record       = "tumi2"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}

##############################################
# Setup Stage 2 Instance
##############################################
module "stage2" {
  source           = "../../modules/gce/appliance"
  instance_name    = "stage2"
  dns_record       = "stage2"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}

##############################################
# Setup Stage 3 Instance
##############################################
module "stage3" {
  source           = "../../modules/gce/appliance"
  instance_name    = "stage3"
  dns_record       = "stage3"
  appliance_image  = "${var.appliance_image}"
  consul_leader    = "${var.consul_leader}"
  ssh_user         = "${var.ssh_user}"
  ssh_private_key  = "${var.ssh_private_key}"
  dnsimple_account = "${var.dnsimple_account}"
  dnsimple_token   = "${var.dnsimple_token}"
}
