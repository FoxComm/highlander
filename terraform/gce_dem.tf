
variable "ssh_user" {} 
variable "ssh_private_key" {} 

module "dem1" {
    source = "./gce/demostack"
    prefix = "dem1"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "dem2" {
    source = "./gce/demostack"
    prefix = "dem2"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
