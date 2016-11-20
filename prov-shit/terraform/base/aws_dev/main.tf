variable "aws_access_key" {}

variable "aws_secret_key" {}

variable "aws_ssh_user" {}

variable "aws_ssh_private_key" {}

variable "key_name" {}

variable "region" {}

variable "public_subnet_id" {}

variable "private_subnet_id" {}

variable "public_security_groups" {
  type = "list"
}

variable "private_security_groups" {
  type = "list"
}

variable "amigo_leader_image" {}

variable "amigo_leader_datacenter" {}

variable "amigo_image" {}

variable "amigo_instance_type" {}

variable "amigo_volume_size" {}

variable "backend_image" {}

variable "backend_instance_type" {}

variable "backend_volume_size" {}

variable "frontend_image" {}

variable "frontend_instance_type" {}

variable "frontend_volume_size" {}

variable "dnsimple_token" {}

variable "dnsimple_email" {}

provider "aws" {
  access_key = "${var.aws_access_key}"
  secret_key = "${var.aws_secret_key}"
  region     = "${var.region}"
}

provider "dnsimple" {
  token = "${var.dnsimple_token}"
  email = "${var.dnsimple_email}"
}

module "dev" {
  source              = "../../modules/aws/dev"
  aws_ssh_user        = "${var.aws_ssh_user}"
  aws_ssh_private_key = "${var.aws_ssh_private_key}"
  datacenter          = "dev"
  dns_entry           = "consul.aws-dev"
  key_name            = "${var.key_name}"

  subnet_id       = "${var.private_subnet_id}"
  security_groups = "${var.private_security_groups}"

  image         = "${var.amigo_leader_image}"
  instance_type = "${var.amigo_instance_type}"
  volume_size   = "${var.amigo_volume_size}"
}

module "tgt-wild" {
  source              = "../../modules/aws/tinystack"
  aws_ssh_user        = "${var.aws_ssh_user}"
  aws_ssh_private_key = "${var.aws_ssh_private_key}"
  consul_leader       = "${module.dev.leader}"
  datacenter          = "tgt-wild"
  key_name            = "${var.key_name}"

  public_subnet_id        = "${var.public_subnet_id}"
  private_subnet_id       = "${var.private_subnet_id}"
  public_security_groups  = "${var.public_security_groups}"
  private_security_groups = "${var.private_security_groups}"

  amigo_image         = "${var.amigo_image}"
  amigo_instance_type = "${var.amigo_instance_type}"
  amigo_volume_size   = "${var.amigo_volume_size}"

  backend_image         = "${var.backend_image}"
  backend_instance_type = "${var.backend_instance_type}"
  backend_volume_size   = "${var.backend_volume_size}"

  frontend_image         = "${var.frontend_image}"
  frontend_instance_type = "${var.frontend_instance_type}"
  frontend_volume_size   = "${var.frontend_volume_size}"
}

module "tgt" {
  source              = "../../modules/aws/tinystack"
  aws_ssh_user        = "${var.aws_ssh_user}"
  aws_ssh_private_key = "${var.aws_ssh_private_key}"
  consul_leader       = "${module.dev.leader}"
  datacenter          = "tgt"
  key_name            = "${var.key_name}"

  public_subnet_id        = "${var.public_subnet_id}"
  private_subnet_id       = "${var.private_subnet_id}"
  public_security_groups  = "${var.public_security_groups}"
  private_security_groups = "${var.private_security_groups}"

  amigo_image         = "${var.amigo_image}"
  amigo_instance_type = "${var.amigo_instance_type}"
  amigo_volume_size   = "${var.amigo_volume_size}"

  backend_image         = "${var.backend_image}"
  backend_instance_type = "${var.backend_instance_type}"
  backend_volume_size   = "${var.backend_volume_size}"

  frontend_image         = "${var.frontend_image}"
  frontend_instance_type = "${var.frontend_instance_type}"
  frontend_volume_size   = "${var.frontend_volume_size}"
}
