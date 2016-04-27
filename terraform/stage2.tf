
variable "backend_image" {
    default = "tinystack-backend-1461738064"
} 

variable "frontend_image" {
    default = ""
} 

variable "ssh_user" {} 
variable "ssh_private_key" {} 

module "stage2" {
    source = "./tinystack"
    prefix = "stage2"
    datacenter = "stage"
    backend_image = "${var.backend_image}"
    frontend_image = "${var.frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
