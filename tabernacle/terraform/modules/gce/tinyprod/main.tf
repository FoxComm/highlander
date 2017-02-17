variable "datacenter" {}

variable "backend_image" {}

variable "frontend_image" {}

variable "ssh_user" {}

variable "ssh_private_key" {}

variable "amigo_server_image" {}

variable "frontend_machine_type" {
  default = "n1-highcpu-8"
}

variable "network" {
  default = "default"
}

resource "google_compute_instance" "tinyprod-amigo" {
  name         = "${var.datacenter}-amigo"
  machine_type = "n1-standard-1"
  tags         = ["no-ip", "${var.datacenter}-amigo", "${var.datacenter}"]
  zone         = "us-central1-a"

  metadata {
    consul_dc = "${var.datacenter}"
  }

  disk {
    image = "${var.amigo_server_image}"
    type  = "pd-ssd"
    size  = "40"
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

resource "google_compute_instance" "tinyprod-frontend" {
  name         = "${var.datacenter}-frontend"
  machine_type = "${var.frontend_machine_type}"
  tags         = ["no-ip", "http-server", "https-server", "${var.datacenter}-frontend"]
  zone         = "us-central1-a"

  disk {
    image = "${var.frontend_image}"
    type  = "pd-ssd"
    size  = "100"
  }

  network_interface {
    network = "${var.network}"
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

resource "google_compute_instance" "tinyprod-backend" {
  name         = "${var.datacenter}-backend"
  machine_type = "n1-highmem-4"
  tags         = ["no-ip", "${var.datacenter}-backend"]
  zone         = "us-central1-a"

  disk {
    image = "${var.backend_image}"
    type  = "pd-ssd"
    size  = "500"
  }

  network_interface {
    network = "${var.network}"
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
