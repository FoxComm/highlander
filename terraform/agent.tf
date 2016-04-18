
variable "ssh_user" {} 
variable "ssh_private_key" {} 

module "agent1" {
    source = "./agent"
    prefix = "agent1"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "agent2" {
    source = "./agent"
    prefix = "agent2"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
