variable "ssh_user" {}
variable "ssh_private_key" {}

variable "account_file" {}
variable "gce_project" {}
variable "region" {}
variable "base_image" {}
variable "amigo_image" {}
variable "tiny_backend_image" {}
variable "tiny_frontend_image" {}
variable "gatling_image" {}
variable "builder_image" {}

provider "google"
{
    credentials = "${file(var.account_file)}"
    project = "${var.gce_project}"
    region = "${var.region}"
}

##############################################
# Setup Consul Cluster
##############################################
module "consul_cluster" {
    source = "../../modules/gce/amigos"
    datacenter = "dev"
    network = "default"
    servers = 3
    image = "${var.amigo_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

##############################################
# Setup Buildkite Agent Machines
##############################################
module "buildagents" {
    source = "../../modules/gce/agent"
    prefix = "buildkite-agent"
    queue = "core"
    network = "default"
    image = "${var.builder_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    servers = 8
}
