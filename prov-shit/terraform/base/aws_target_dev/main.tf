variable "aws_access_key" {}
variable "aws_secret_key" {}
variable "ssh_user" {}
variable "ssh_private_key" {}
variable "aws_key_name" {}
variable "region" {}
variable "public_subnet_cidr" {}
variable "private_subnet_cidr" {}

variable "base_image" {}
variable "amigo_cluster_image" {}
variable "kafka_image" {}
variable "db_image" {}
variable "es_image" {}
variable "log_image" {}
variable "phoenix_image" {}
variable "greenriver_image" {}
variable "front_image" {}
variable "front_workers" {}
variable "service_worker_image" {}
variable "service_workers" {}

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

module "target_development" {
    source = "../../modules/aws/vanilla"
    vpc_cidr = "10.0.0.0/16"
    public_subnet_cidr = "${var.public_subnet_cidr}"
    private_subnet_cidr = "${var.private_subnet_cidr}"
    region = "${var.region}"
    
    name = "tgt"
    key_name = "${var.aws_key_name}"
    
    base_image = "${var.base_image}"
    amigo_cluster_image = "${var.amigo_cluster_image}"
    kafka_image = "${var.kafka_image}"
    db_image = "${var.db_image}"
    es_image = "${var.es_image}"
    log_image = "${var.log_image}"
    phoenix_image = "${var.phoenix_image}"
    greenriver_image = "${var.greenriver_image}"
    front_image = "${var.front_image}"
    front_workers = "${var.front_workers}"
    service_worker_image = "${var.service_worker_image}"
    service_workers = "${var.service_workers}"
    stage_amigo_image = "${var.stage_amigo_image}"
    stage_frontend_image = "${var.stage_frontend_image}"
    stage_backend_image = "${var.stage_backend_image}"
}
