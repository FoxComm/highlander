
variable "image" { 
    default = "consul-server-1461715686"
}

variable "ssh_user" {} 
variable "ssh_private_key" {} 

module "consul_cluster" {
    source = "./gce/consul"
    datacenter = "dev"
    servers = 3
    image = "${var.image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

