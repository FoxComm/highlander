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

variable "dnsimple_token" {}

variable "dnsimple_email" {}

variable "demo1_public_ip" {}

variable "demo2_public_ip" {}

provider "google" {
  credentials = "${file(var.account_file)}"
  project     = "${var.gce_project}"
  region      = "${var.region}"
}

##############################################
# Setup Demo Tiny Stack
##############################################
module "demo1" {
  source                = "../../modules/gce/tinystack"
  datacenter            = "demo1"
  backend_image         = "${var.tiny_backend_image}"
  frontend_image        = "${var.tiny_frontend_image}"
  ssh_user              = "${var.ssh_user}"
  ssh_private_key       = "${var.ssh_private_key}"
  consul_leader         = "${var.consul_leader}"
  consul_server_image   = "${var.consul_server_image}"
  frontend_public_ip    = "${var.demo1_public_ip}"
  frontend_machine_type = "n1-highmem-8"
}

module "demo2" {
  source                = "../../modules/gce/tinystack"
  datacenter            = "demo2"
  backend_image         = "${var.tiny_backend_image}"
  frontend_image        = "${var.tiny_frontend_image}"
  ssh_user              = "${var.ssh_user}"
  ssh_private_key       = "${var.ssh_private_key}"
  consul_leader         = "${var.consul_leader}"
  consul_server_image   = "${var.consul_server_image}"
  frontend_public_ip    = "${var.demo2_public_ip}"
  frontend_machine_type = "n1-highmem-8"
}

##############################################
# Setup DNS
##############################################
provider "dnsimple" {
  token = "${var.dnsimple_token}"
  email = "${var.dnsimple_email}"
}

resource "dnsimple_record" "frontend-dns-record" {
  domain = "foxcommerce.com"
  name   = "demo1"
  value  = "${module.demo1.public_frontend_address}"
  type   = "A"
  ttl    = 3600
}

resource "dnsimple_record" "frontend-dns-record-2" {
  domain = "foxcommerce.com"
  name   = "demo2"
  value  = "${module.demo2.public_frontend_address}"
  type   = "A"
  ttl    = 3600
}
