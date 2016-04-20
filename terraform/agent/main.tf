variable "demo_image" { 
    default = "ubuntu-1510-wily-v20160123"
}

variable "queue" {}
variable "prefix" {} 
variable "ssh_user" {} 
variable "ssh_private_key" {} 
variable "group_size" {} 

resource "google_compute_instance_template" "build_agent" { 
    name = "${var.prefix}"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "${var.prefix}"]

    disk {
        source_image = "${var.demo_image}"
        disk_type = "pd-ssd"
        disk_size_gb = "100"
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

resource "google_compute_instance_group_manager" "build_group_manager" {
    description = "Build agent instance group"
    name = "${var.prefix}-${var.queue}-group"
    instance_template = "${google_compute_instance_template.build_agent.self_link}"
    update_strategy= "NONE"
    base_instance_name = "${var.prefix}-${var.queue}"
    zone = "us-central1-a"
    target_size = "${var.group_size}"
}
