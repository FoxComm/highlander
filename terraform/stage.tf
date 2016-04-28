
variable "backend_image" {
    default = "tinystack-backend-1461799315"
} 

variable "frontend_image" {
    default = "tinystack-frontend-1461787500"
} 

variable "consul_server_image" { 
    default = "tinystack-consul-server-1461788045"
}

variable "ssh_user" {} 
variable "ssh_private_key" {} 

module "stage" {
    source = "./tinystack"
    datacenter = "stage"
    backend_image = "${var.backend_image}"
    frontend_image = "${var.frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${module.consul_cluster.leader}"
    consul_server_image = "${var.consul_server_image}"
}
