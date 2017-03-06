variable "ssh_user" {}

variable "ssh_private_key" {}

variable "account_file" {}

variable "gce_project" {}

variable "region" {}

variable "appliance_image" {}

variable "agent_image" {}

variable "consul_leader" {}

variable "dnsimple_token" {}

variable "dnsimple_email" {}

provider "google" {
  credentials = "${file(var.account_file)}"
  project     = "${var.gce_project}"
  region      = "${var.region}"
}

##############################################
# Setup Cloud Central Server
##############################################
module "cloud-central" {
  source          = "../../modules/gce/cloud"
  instance_name   = "cloud-central"
  dns_record      = "cloud"
  agent_image     = "${var.agent_image}"
  ssh_user        = "${var.ssh_user}"
  ssh_private_key = "${var.ssh_private_key}"
  dnsimple_email  = "${var.dnsimple_email}"
  dnsimple_token  = "${var.dnsimple_token}"
}

##############################################
# Setup Cloud Demo Appliance
##############################################
module "cloud-demo" {
  source          = "../../modules/gce/appliance"
  instance_name   = "cloud-demo"
  dns_record      = "example"
  appliance_image = "${var.appliance_image}"
  consul_leader   = "${var.consul_leader}"
  ssh_user        = "${var.ssh_user}"
  ssh_private_key = "${var.ssh_private_key}"
  dnsimple_email  = "${var.dnsimple_email}"
  dnsimple_token  = "${var.dnsimple_token}"
}
