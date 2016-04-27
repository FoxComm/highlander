variable "datacenter" {} 
variable "backend_image" {} 
variable "frontend_image" {} 
variable "prefix" {} 
variable "ssh_user" {} 
variable "ssh_private_key" {} 

resource "google_compute_instance" "tiny-ashes" { 
    name = "${var.prefix}-ashes"
    machine_type = "n1-highcpu-8"
    tags = ["http-server", "https-server", "${var.prefix}-ashes"]
    zone = "us-central1-a"

    disk {
        image = "${var.frontend_image}"
        type = "pd-ssd"
        size = "30"
    }   

    network_interface {
        network = "default"
        access_config {
        }
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
          "/tmp/consul.sh ${var.datacenter} ${google_compute_instance.consul_server.0.network_interface.0.address}"
        ]
    }
}

resource "google_compute_instance" "tiny-backend" { 
    name = "${var.prefix}-backend"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "${var.prefix}-backend"]
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

    provisioner "remote-exec" {
        script = "terraform/scripts/bootstrap.sh"
    }
}
