variable "demo_image" { 
    default = "ubuntu-1510-wily-v20160123"
}

variable "queue" {}
variable "prefix" {} 
variable "ssh_user" {} 
variable "ssh_private_key" {} 

resource "google_compute_instance" "agent" { 
    name = "${var.prefix}-${var.queue}"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "${var.prefix}"]
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
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "file" {
        source = "terraform/scripts/agent.sh"
        destination = "/tmp/provision.sh"
    }

    provisioner "remote-exec" {
        inline = [
          "chmod +x /tmp/provision.sh",
          "/tmp/provision.sh ${var.queue}"
        ]
    }
}
