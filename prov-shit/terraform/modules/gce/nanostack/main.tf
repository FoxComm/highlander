variable "ssh_user" {}
variable "ssh_private_key" {}

variable "network" {}
variable "zone" {}

variable "subdomain" {}
variable "datacenter" {}
variable "join_type" {}

variable "nano_amigo_image" {}
variable "nano_storage_image" {}
variable "nano_worker_image" {}

resource "google_compute_instance" "amigo" {
    name = "${var.datacenter}-amigo"
    machine_type = "n1-standard-2"
    tags = ["no-ip", "ssh", "http-server", "https-server", "${var.datacenter}"]
    zone = "${var.zone}"

    disk {
        image = "${var.nano_amigo_image}"
        type = "pd-ssd"
        size = "10"
    }

    network_interface {
        network = "${var.network}"
    }

    connection {
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${module.nanostack.consul_address} ${var.join_type}",
        ]
    }
}

resource "google_compute_instance" "storage" {
    name = "${var.datacenter}-storage"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "ssh", "${var.datacenter}"]
    zone = "${var.zone}"

    disk {
        image = "${var.nano_storage_image}"
        type = "pd-ssd"
        size = "30"
    }

    network_interface {
        network = "${var.network}"
    }

    connection {
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${module.nanostack.consul_address} ${var.join_type}"
        ]
    }
}

resource "google_compute_instance" "worker" {
    name = "${var.datacenter}-worker"
    machine_type = "n1-highmem-8"
    tags = ["no-ip", "ssh", "${var.datacenter}"]
    zone = "${var.zone}"

    disk {
        image = "${var.nano_worker_image}"
        type = "pd-ssd"
        size = "100"
    }

    network_interface {
        network = "${var.network}"
    }

    service_account {
        scopes = ["storage-ro"]
    }

    connection {
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${module.nanostack.consul_address} ${var.join_type}"
        ]
    }
}

resource "dnsimple_record" "dns_record" {
    domain = "foxcommerce.com"
    name = "${var.subdomain}"
    value = "${module.nanostack.consul_address}"
    type = "A"
    ttl = 3600
}
