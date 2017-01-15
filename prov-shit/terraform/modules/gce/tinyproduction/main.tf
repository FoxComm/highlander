##############################################
# SSH Parameters
##############################################
variable "ssh_user" {}

variable "ssh_private_key" {}

##############################################
# Primary Parameters
##############################################
variable "datacenter" {}

variable "network" {}

variable "amigo_image" {}

variable "backend_image" {}

variable "frontend_image" {}

##############################################
# Secondary Parameters
##############################################
variable "bucket_location" {
  default = "us"
}

variable "dnssimple_domain" {
  default = "foxcommerce.com"
}

variable "zone" {
  default = "us-central1-a"
}

##############################################
# Hardware Configurations
##############################################
variable "amigo_machine_type" {
  default = "n1-standard-2"
}

variable "amigo_disk_size" {
  default = "20"
}

variable "backend_machine_type" {
  default = "n1-standard-8"
}

variable "backend_disk_size" {
  default = "200"
}

variable "frontend_machine_type" {
  default = "n1-standard-8"
}

variable "frontend_disk_size" {
  default = "60"
}

##############################################
# Storage Buckets
##############################################
resource "google_storage_bucket" "backups" {
  name     = "${var.datacenter}-backups"
  location = "${var.bucket_location}"
}

resource "google_storage_bucket" "registry" {
  name     = "${var.datacenter}-docker"
  location = "${var.bucket_location}"
}

##############################################
# Amigo Server
##############################################
resource "google_compute_instance" "tinyprod-amigo" {
  name         = "${var.datacenter}-amigo"
  machine_type = "${var.amigo_machine_type}"
  zone         = "${var.zone}"
  tags         = ["no-ip"]

  disk {
    image = "${var.amigo_image}"
    size  = "${var.amigo_disk_size}"
    type  = "pd-ssd"
  }

  network_interface {
    network = "${var.network}"
  }

  service_account {
    scopes = ["storage-rw"]
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.tinyprod-amigo.network_interface.0.address}",
      "sudo rm -rf /var/consul/* && sudo systemctl restart consul_server.service",
    ]
  }
}

##############################################
# Backend Worker
##############################################
resource "google_compute_instance" "tinyprod-backend" {
  name         = "${var.datacenter}-backend"
  machine_type = "${var.backend_machine_type}"
  zone         = "${var.zone}"
  tags         = ["no-ip"]

  disk {
    image = "${var.backend_image}"
    size  = "${var.backend_disk_size}"
    type  = "pd-ssd"
  }

  network_interface {
    network = "${var.network}"
  }

  service_account {
    scopes = ["storage-rw"]
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.tinyprod-amigo.network_interface.0.address}",
    ]
  }
}

##############################################
# Frontend Worker
##############################################
resource "google_compute_instance" "tinyprod-frontend" {
  name         = "${var.datacenter}-frontend"
  machine_type = "${var.frontend_machine_type}"
  zone         = "${var.zone}"
  tags         = ["http-server", "https-server"]

  disk {
    image = "${var.frontend_image}"
    size  = "${var.frontend_disk_size}"
    type  = "pd-ssd"
  }

  network_interface {
    network = "${var.network}"
  }

  service_account {
    scopes = ["storage-rw"]
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.tinyprod-amigo.network_interface.0.address}",
    ]
  }
}
