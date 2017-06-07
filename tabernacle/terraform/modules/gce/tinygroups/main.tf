variable "ssh_user" {}

variable "ssh_private_key" {}

variable "datacenter" {}

variable "amigo_image" {}

variable "backend_image" {}

variable "frontend_image" {}

variable "consul_leader" {}

resource "google_compute_instance" "tiny-amigo" {
  name         = "${var.datacenter}-amigo"
  machine_type = "n1-standard-1"
  tags         = ["no-ip"]
  zone         = "us-central1-a"

  service_account {
    scopes = ["storage-rw"]
  }

  disk {
    image = "${var.amigo_image}"
    type  = "pd-ssd"
    size  = "30"
  }

  network_interface {
    network = "default"
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${var.consul_leader}",
      "sudo rm -rf /var/consul/* && sudo systemctl restart consul_server",
    ]
  }
}

resource "google_compute_instance" "tiny-frontend" {
  name         = "${var.datacenter}-frontend"
  machine_type = "n1-highcpu-8"
  tags         = ["http-server", "https-server"]
  zone         = "us-central1-a"

  service_account {
    scopes = ["storage-rw"]
  }

  disk {
    image = "${var.frontend_image}"
    type  = "pd-ssd"
    size  = "100"
  }

  network_interface {
    network = "default"
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.tiny-amigo.network_interface.0.address}",
    ]
  }
}

resource "google_compute_instance" "tiny-backend" {
  name         = "${var.datacenter}-backend"
  machine_type = "n1-highmem-4"
  tags         = ["no-ip"]
  zone         = "us-central1-a"

  service_account {
    scopes = ["storage-rw"]
  }

  disk {
    image = "${var.backend_image}"
    type  = "pd-ssd"
    size  = "100"
  }

  network_interface {
    network = "default"
  }

  connection {
    type        = "ssh"
    user        = "${var.ssh_user}"
    private_key = "${file(var.ssh_private_key)}"
  }

  provisioner "remote-exec" {
    inline = [
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.tiny-amigo.network_interface.0.address}",
    ]
  }
}
