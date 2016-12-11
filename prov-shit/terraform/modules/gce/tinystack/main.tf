variable "datacenter" {}
variable "backend_image" {}
variable "frontend_image" {}
variable "ssh_user" {}
variable "ssh_private_key" {}
variable "consul_leader" {}
variable "consul_server_image" {}

variable "frontend_machine_type" {
    default = "n1-highcpu-8"
}

variable "frontend_disk_size" {
    default = "30"
}

resource "google_compute_instance" "tiny-consul" {
    name = "${var.datacenter}-consul-server"
    machine_type = "n1-standard-1"
    tags = ["no-ip", "${var.datacenter}-consul-server", "${var.datacenter}"]
    zone = "us-central1-a"

    service_account {
        scopes = ["storage-rw"]
    }

    disk {
        image = "${var.consul_server_image}"
        type = "pd-ssd"
        size = "20"
    }

    network_interface {
        network = "default"
    }

    connection {
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap.sh",
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${var.consul_leader}",
          "sudo rm -rf /var/consul/* && sudo systemctl restart consul_server.service",
		  "/opt/sensu/embedded/bin/update-client-cfg.rb ${var.datacenter}-consul-server ${google_compute_instance.tiny-consul.network_interface.0.address}"
        ]
    }

}

#resource "google_compute_instance" "tiny-frontend" {
#    name = "${var.datacenter}-frontend"
#    machine_type = "${var.frontend_machine_type}"
#    tags = ["no-ip", "http-server", "https-server", "${var.datacenter}-frontend"]
#    zone = "us-central1-a"

#    disk {
#        image = "${var.frontend_image}"
#        type = "pd-ssd"
#        size = "${var.frontend_disk_size}"
#    }

#    network_interface {
#        network = "default"
#    }

#    connection {
#        type = "ssh"
#        user = "${var.ssh_user}"
#        private_key = "${file(var.ssh_private_key)}"
#    }

#    provisioner "remote-exec" {
#        inline = [
#          "/usr/local/bin/bootstrap.sh",
#          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.tiny-consul.network_interface.0.address}"
#        ]
#    }
#}

#resource "google_compute_instance" "tiny-backend" {
#    name = "${var.datacenter}-backend"
#    machine_type = "n1-highmem-4"
#    tags = ["no-ip", "${var.datacenter}-backend"]
#    zone = "us-central1-a"

#    disk {
#        image = "${var.backend_image}"
#        type = "pd-ssd"
#        size = "100"
#    }

#    network_interface {
#        network = "default"
#    }

#    connection {
#        type = "ssh"
#        user = "${var.ssh_user}"
#        private_key = "${file(var.ssh_private_key)}"
#    }

#    provisioner "remote-exec" {
#        inline = [
#          "/usr/local/bin/bootstrap.sh",
#          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.tiny-consul.network_interface.0.address}"
#        ]
#    }
#}
