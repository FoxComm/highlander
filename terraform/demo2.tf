variable "demo_image" { 
    default = "ubuntu-1510-wily-v20160123"
}

variable "ssh_user" {} 
variable "ssh_private_key" {} 

resource "google_compute_instance" "demo2-ashes" { 
    name = "demo2-ashes"
    machine_type = "n1-highcpu-8"
    tags = ["http-server", "https-server", "demo2-ashes"]
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
        script = "terraform/demostack/scripts/bootstrap.sh"
    }
}

resource "google_compute_instance" "demo2-backend" { 
    name = "demo2-backend"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "demo2-backend"]
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
        script = "terraform/demostack/scripts/bootstrap.sh"
    }
}
