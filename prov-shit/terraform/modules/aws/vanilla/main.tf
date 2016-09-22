variable "vpc_cidr" {}
variable "name" {}
variable "key_name" {}
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