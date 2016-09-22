variable "aws_access_key" {}
variable "aws_secret_key" {}
variable "ssh_user" {}
variable "ssh_private_key" {}
variable "aws_key_name" {}
variable "region" {}

variable "base_image" {}
variable "amigo_cluster_image" {}
variable "kafka_image" {}
variable "db_image" {}
variable "es_image" {}
variable "log_image" {}
variable "phoenix_image" {}
variable "greenriver_image" {}
variable "front_image" {}
variable "service_worker_image" {}

variable "stage_amigo_image" {}
variable "stage_backend_image" {}
variable "stage_frontend_image" {}

provider "aws" {
    access_key = "${var.aws_access_key}"
    secret_key = "${var.aws_secret_key}"
    region = "${var.region}"
}

module "target_amigos" {
    source = "../../modules/aws/amigos"
    image = "${var.amigo_cluster_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    key_name = "${var.aws_key_name}"

    datacenter = "target"  
    subnet_id = "subnet-9094cdf4"
    security_groups = ["sg-e50cd29c", "sg-a021d3d9"]
}

module "target_staging" {
    amigo_leader = "${module.target_amigos.leader}"

    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"    
    source = "../../modules/aws/vanilla_stage"
    key_name = "${var.aws_key_name}"

    stage_amigo_image = "${var.stage_amigo_image}"
    stage_frontend_image = "${var.stage_frontend_image}"
    stage_backend_image = "${var.stage_backend_image}"

    stage_datacenter = "target-stage"  
    subnet_id = "subnet-9094cdf4"
    security_groups = ["sg-e50cd29c", "sg-a021d3d9"]    
}
