variable "ssh_user" {}

variable "ssh_private_key" {}

variable "account_file" {}

variable "gce_project" {}

variable "region" {}

variable "amigo_image" {}

variable "backend_image" {}

variable "frontend_image" {}

variable "consul_leader" {}

provider "google" {
  credentials = "${file(var.account_file)}"
  project     = "${var.gce_project}"
  region      = "${var.region}"
}

#############################################
# Setup BVT Test Environment
#############################################
module "foxcomm-test-groups" {
  source                = "../../modules/gce/tinygroups"
  datacenter            = "foxcomm-test"
  amigo_image           = "${var.amigo_image}"
  backend_image         = "${var.backend_image}"
  frontend_image        = "${var.frontend_image}"
  ssh_user              = "${var.ssh_user}"
  ssh_private_key       = "${var.ssh_private_key}"
  consul_leader         = "${var.consul_leader}"
}
