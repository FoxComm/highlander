variable "datacenter" {} 
variable "kafka_image" {} 
variable "db_image" {} 
variable "es_image" {} 
variable "phoenix_image" {} 
variable "greenriver_image" {} 
variable "front_image" {} 
variable "ssh_user" {} 
variable "ssh_private_key" {} 
variable "consul_leader" {} 
variable "network" {} 
variable "zone" {} 

resource "google_compute_instance" "kafka" { 
    name = "${var.datacenter}-kafka"
    machine_type = "n1-standard-2"
    tags = ["ssh", "no-ip", "http-server", "https-server", "${var.datacenter}-kafka", "${var.datacenter}"]
    zone = "${var.zone}"

    disk {
        image = "${var.kafka_image}"
        type = "pd-ssd"
        size = "100"
    }   

    network_interface {
        network = "${var.network}"
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

resource "google_compute_instance" "db" { 
    name = "${var.datacenter}-db"
    machine_type = "n1-highmem-2"
    tags = ["ssh", "no-ip", "http-server", "https-server", "${var.datacenter}-db", "${var.datacenter}"]
    zone = "${var.zone}"

    disk {
        image = "${var.db_image}"
        type = "pd-ssd"
        size = "100"
    }   

    network_interface {
        network = "${var.network}"
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

resource "google_compute_instance" "es" { 
    name = "${var.datacenter}-es"
    machine_type = "n1-highmem-2"
    tags = ["ssh", "no-ip", "http-server", "https-server", "${var.datacenter}-es", "${var.datacenter}"]
    zone = "${var.zone}"

    disk {
        image = "${var.es_image}"
        type = "pd-ssd"
        size = "100"
    }   

    network_interface {
        network = "${var.network}"
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

resource "google_compute_instance" "phoenix" { 
    name = "${var.datacenter}-phoenix"
    machine_type = "n1-standard-4"
    tags = ["ssh", "no-ip", "http-server", "https-server", "${var.datacenter}-phoenix", "${var.datacenter}"]
    zone = "${var.zone}"

    disk {
        image = "${var.phoenix_image}"
        type = "pd-ssd"
        size = "10"
    }   

    network_interface {
        network = "${var.network}"
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

resource "google_compute_instance" "greenriver" { 
    name = "${var.datacenter}-greenriver"
    machine_type = "n1-standard-2"
    tags = ["ssh", "no-ip", "http-server", "https-server", "${var.datacenter}-greenriver", "${var.datacenter}"]
    zone = "${var.zone}"

    disk {
        image = "${var.greenriver_image}"
        type = "pd-ssd"
        size = "10"
    }   

    network_interface {
        network = "${var.network}"
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

resource "google_compute_instance" "front" { 
    name = "${var.datacenter}-front"
    machine_type = "n1-standard-2"
    tags = ["ssh", "no-ip", "http-server", "https-server", "${var.datacenter}-front", "${var.datacenter}"]
    zone = "${var.zone}"

    disk {
        image = "${var.front_image}"
        type = "pd-ssd"
        size = "10"
    }   

    network_interface {
        network = "${var.network}"
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
