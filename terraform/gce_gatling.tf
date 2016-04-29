
variable "backend_image" {
    default = "tinystack-backend-1461799315"
} 

variable "frontend_image" {
    default = "tinystack-frontend-1461787500"
} 

variable "consul_server_image" { 
    default = "tinystack-consul-server-1461881696"
}

variable "ssh_user" {} 
variable "ssh_private_key" {} 

module "gatling" {
    source = "./gce/tinystack"
    datacenter = "gatling"
    backend_image = "${var.backend_image}"
    frontend_image = "${var.frontend_image}"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
    consul_leader = "${module.consul_cluster.leader}"
    consul_server_image = "${var.consul_server_image}"
}

variable "gatling_image" { 
    default = "base-jvm-1461863900"
}

resource "google_compute_instance" "gatling-gun"{ 
    name = "gatling-gun"
    machine_type = "n1-standard-4"
    tags = ["no-ip", "gatling-gun"]
    zone = "us-central1-a"

    disk {
        image = "${var.gatling_image}"
        type = "pd-ssd"
        size = "30"
    }   

    network_interface {
        network = "default"
    }

    connection { 
        type = "ssh"
        user = "${var.ssh_user}"
        private_key="${file(var.ssh_private_key)}"
    }

    provisioner "file" {
        source = "terraform/scripts/bootstrap.sh"
        destination = "/tmp/bootstrap.sh"
    }

    provisioner "file" {
        source = "terraform/scripts/consul.sh"
        destination = "/tmp/consul.sh"
    }

    provisioner "remote-exec" {
        inline = [
          "chmod +x /tmp/bootstrap.sh",
          "chmod +x /tmp/consul.sh",
          "/tmp/bootstrap.sh",
          "/tmp/consul.sh gatling ${module.gatling.consul_address}"
        ]
    }
}
