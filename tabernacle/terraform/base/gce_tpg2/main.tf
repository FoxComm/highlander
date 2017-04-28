variable "ssh_user" {}

variable "ssh_private_key" {}

variable "account_file" {}

variable "gce_project" {}

variable "region" {}

variable "zone" {}

variable "vpn_image" {}

variable "network" {}

provider "google" {
  credentials = "${file(var.account_file)}"
  project     = "${var.gce_project}"
  region      = "${var.region}"
}

##############################################
# Production Cluster
##############################################
module "tpg_production2" {
  source                = "../../modules/gce/tinyproduction"
  ssh_user              = "${var.ssh_user}"
  ssh_private_key       = "${var.ssh_private_key}"
  network               = "${var.network}"
  datacenter            = "tpg-production2"
  amigo_image           = "base-amigo-170428-054708"
  backend_image         = "base-backend-170428-055839"
  frontend_image        = "base-frontend-170428-055821"
  amigo_disk_size       = "40"
  backend_machine_type  = "n1-standard-8"
  backend_disk_size     = "300"
  frontend_machine_type = "n1-standard-4"
  frontend_disk_size    = "100"
}
