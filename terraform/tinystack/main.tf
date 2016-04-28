variable "datacenter" {} 
variable "backend_image" {} 
variable "frontend_image" {} 
variable "ssh_user" {} 
variable "ssh_private_key" {} 
variable "consul_leader" {} 
variable "consul_server_image" {} 

resource "google_compute_instance" "tiny-consul" { 
    name = "${var.datacenter}-consul-server"
    machine_type = "n1-standard-1"
    tags = ["no-ip", "${var.datacenter}-consul-server", "${var.datacenter}-consol-server", "${var.datacenter}"]
    zone = "us-central1-a"

    metadata { 
        consul_dc = "${var.datacenter}"
    }

    disk {
        image = "${var.consul_server_image}"
        type = "pd-ssd"
        size = "10"
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
          "/tmp/consul.sh ${var.datacenter} ${var.consul_leader}"
        ]
    }

}

resource "google_compute_instance" "tiny-frontend" { 
    name = "${var.datacenter}-frontend"
    machine_type = "n1-highcpu-8"
    tags = ["no-ip", "http-server", "https-server", "${var.datacenter}-frontend"]
    zone = "us-central1-a"

    disk {
        image = "${var.frontend_image}"
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
          "/tmp/consul.sh ${var.datacenter} ${google_compute_instance.tiny-consul.network_interface.0.address}"
        ]
    }
}

resource "google_compute_instance" "tiny-backend" { 
    name = "${var.datacenter}-backend"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "${var.datacenter}-backend"]
    zone = "us-central1-a"

    disk {
        image = "${var.backend_image}"
        type = "pd-ssd"
        size = "100"
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
          "/tmp/consul.sh ${var.datacenter} ${google_compute_instance.tiny-consul.network_interface.0.address}"
        ]
    }
}

