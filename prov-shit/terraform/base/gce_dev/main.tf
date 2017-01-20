variable "ssh_user" {}

variable "ssh_private_key" {}

variable "dnsimple_token" {}

variable "dnsimple_email" {}

variable "account_file" {}

variable "gce_project" {}

variable "region" {}

variable "base_image" {}

variable "amigo_image" {}

variable "tiny_backend_image" {}

variable "tiny_frontend_image" {}

variable "gatling_image" {}

variable "builder_image" {}

provider "google" {
  credentials = "${file(var.account_file)}"
  project     = "${var.gce_project}"
  region      = "${var.region}"
}

provider "dnsimple" {
  token = "${var.dnsimple_token}"
  email = "${var.dnsimple_email}"
}

##############################################
# Setup Consul Cluster
##############################################
module "consul_cluster" {
  source          = "../../modules/gce/amigos"
  datacenter      = "dev"
  network         = "default"
  servers         = 3
  image           = "${var.amigo_image}"
  ssh_user        = "${var.ssh_user}"
  ssh_private_key = "${var.ssh_private_key}"
}

##############################################
# Setup Buildkite Agent Machines
##############################################
module "buildagents" {
  source          = "../../modules/gce/agent"
  prefix          = "buildkite-agent"
  queue           = "core"
  network         = "default"
  servers         = 6
  image           = "${var.builder_image}"
  ssh_user        = "${var.ssh_user}"
  ssh_private_key = "${var.ssh_private_key}"
}

##############################################
# Setup Sinopia Server
##############################################
module "sinopia" {
  source          = "../../modules/gce/sinopia"
  network         = "default"
  datacenter      = "dev"
  image           = "base-node-1470785359"
  amigo_leader    = "10.240.0.10"
  domain          = "foxcommerce.com"
  subdomain       = "npm"
  ssh_user        = "${var.ssh_user}"
  ssh_private_key = "${var.ssh_private_key}"
  dnsimple_token  = "${var.dnsimple_token}"
  dnsimple_email  = "${var.dnsimple_email}"
}
