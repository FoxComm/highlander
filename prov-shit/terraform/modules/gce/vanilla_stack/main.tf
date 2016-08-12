variable "bucket_location" {}
variable "datacenter" {}
variable "stage_datacenter" {}
variable "kafka_image" {}
variable "db_image" {}
variable "es_image" {}
variable "log_image" {}
variable "phoenix_image" {}
variable "greenriver_image" {}
variable "front_image" {}
variable "stage_frontend_image" {}
variable "stage_backend_image" {}
variable "service_worker_image" {}
variable "service_workers" {}
variable "front_workers" {}
variable "ssh_user" {}
variable "ssh_private_key" {}
variable "amigo_leader" {}
variable "stage_amigo_image" {}
variable "network" {}
variable "zone" {}

resource "google_storage_bucket" "backups" {
  name     = "${var.datacenter}-backups"
  location = "${var.bucket_location}"
}

resource "google_storage_bucket" "registry" {
  name     = "${var.datacenter}-docker"
  location = "${var.bucket_location}"
}

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

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${var.amigo_leader}"
        ]
    }
}

resource "google_compute_instance" "db" {
    name = "${var.datacenter}-db"
    machine_type = "n1-highmem-4"
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

    service_account {
        scopes = ["storage-rw"]
    }

    connection {
        type = "ssh"
        user = "${var.ssh_user}"
        private_key="${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${var.amigo_leader}"
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

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${var.amigo_leader}"
        ]
    }
}

resource "google_compute_instance" "log" {
    name = "${var.datacenter}-log"
    machine_type = "n1-highmem-2"
    tags = ["ssh", "no-ip", "http-server", "https-server", "${var.datacenter}-log", "${var.datacenter}"]
    zone = "${var.zone}"

    disk {
        image = "${var.log_image}"
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

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${var.amigo_leader}"
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

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${var.amigo_leader}"
        ]
    }
}

resource "google_compute_instance_template" "service_worker_template" {
    name_prefix = "${var.datacenter}-service-worker-template"
    tags = ["ssh", "no-ip", "http-server", "https-server", "service-worker", "${var.datacenter}-service-worker", "${var.datacenter}"]

    machine_type = "n1-standard-4"
    automatic_restart = true

    disk {
        source_image = "${var.service_worker_image}"
        disk_type = "pd-ssd"
        disk_size_gb = "20"
    }

    network_interface {
        network = "${var.network}"
    }

    service_account {
        scopes = ["storage-ro"]
    }

    lifecycle { 
        create_before_destroy = true
    }

    metadata {
        startup-script = "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${var.amigo_leader}"
    } 
}

resource "google_compute_instance_group_manager" "service_worker_group_manager" {
    name = "${var.datacenter}-service-worker-group-manager"
    base_instance_name = "${var.datacenter}-service-worker"
    instance_template = "${google_compute_instance_template.service_worker_template.self_link}"
    target_size = "${var.service_workers}"
    zone = "${var.zone}"
    
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

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${var.amigo_leader}"
        ]
    }
}

resource "google_compute_instance_template" "front_template" {
    name_prefix = "${var.datacenter}-front-template-"
    machine_type = "n1-standard-2"
    tags = ["ssh", "no-ip", "http-server", "https-server", "${var.datacenter}-front", "${var.datacenter}"]

    disk {
        source_image = "${var.front_image}"
        type = "pd-ssd"
        disk_size_gb = "20"
    }

    network_interface {
        network = "${var.network}"
    }

    lifecycle { 
        create_before_destroy = true
    }

    metadata {
        startup-script = "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${var.amigo_leader}"
    }
}

resource "google_compute_instance_group_manager" "front_group_manager" {
    name = "${var.datacenter}-front-group-manager"
    base_instance_name = "${var.datacenter}-front"
    instance_template = "${google_compute_instance_template.front_template.self_link}"
    target_size = "${var.front_workers}"
    zone = "${var.zone}"

    named_port {
        name = "http-front"
        port = 80
    }
    named_port {
        name = "https-front"
        port = 443
    }
}

resource "google_compute_instance" "stage-amigo" { 
    name = "${var.stage_datacenter}-amigo"
    machine_type = "n1-standard-1"
    tags = ["no-ip", "${var.stage_datacenter}-amigo", "${var.stage_datacenter}"]
    zone = "us-central1-a"

    metadata { 
        consul_dc = "${var.stage_datacenter}"
    }

    disk {
        image = "${var.stage_amigo_image}"
        type = "pd-ssd"
        size = "20"
    }   

    network_interface {
        network = "${var.network}"
    }

    connection { 
        type = "ssh"
        user = "${var.ssh_user}"
        private_key="${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.stage_datacenter} ${var.amigo_leader}"
        ]
    }

}

resource "google_compute_instance" "stage-frontend" { 
    name = "${var.stage_datacenter}-frontend"
    machine_type = "n1-highcpu-8"
    tags = ["no-ip", "http-server", "https-server", "${var.stage_datacenter}-frontend", "${var.stage_datacenter}"]
    zone = "us-central1-a"

    disk {
        image = "${var.stage_frontend_image}"
        type = "pd-ssd"
        size = "30"
    }   

    network_interface {
        network = "${var.network}"
    }

    connection { 
        type = "ssh"
        user = "${var.ssh_user}"
        private_key="${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.stage_datacenter} ${google_compute_instance.stage-amigo.network_interface.0.address}"
        ]
    }
}

resource "google_compute_instance" "stage-backend" { 
    name = "${var.stage_datacenter}-backend"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "${var.stage_datacenter}-backend", "${var.stage_datacenter}"]
    zone = "us-central1-a"

    disk {
        image = "${var.stage_backend_image}"
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

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap_consul.sh ${var.stage_datacenter} ${google_compute_instance.stage-amigo.network_interface.0.address}"
        ]
    }
}

