variable "demo_image" { 
    default = "ubuntu-1510-wily-v20160123"
}

variable "prefix" {} 
variable "ssh_user" {} 
variable "ssh_private_key" {} 

resource "google_compute_instance" "front" { 
    name = "${var.prefix}-front"
    machine_type = "n1-highcpu-8"
    tags = ["http-server", "https-server", "${var.prefix}-front"]
    zone = "us-central1-a"

    disk {
        image = "${var.demo_image}"
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

    provisioner "remote-exec" {
        script = "terraform/scripts/bootstrap.sh"
    }
}


resource "google_compute_instance" "front-ru" { 
    name = "${var.prefix}-front-ru"
    machine_type = "n1-highcpu-8"
    tags = ["http-server", "https-server", "${var.prefix}-front-ru"]
    zone = "us-central1-a"

    disk {
        image = "${var.demo_image}"
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

    provisioner "remote-exec" {
        script = "terraform/scripts/bootstrap.sh"
    }
}

resource "google_compute_instance" "backend" { 
    name = "${var.prefix}-backend"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "${var.prefix}-backend"]
    zone = "us-central1-a"

    disk {
        image = "${var.demo_image}"
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
