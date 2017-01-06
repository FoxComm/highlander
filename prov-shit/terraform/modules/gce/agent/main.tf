variable "image" {}

variable "queue" {}

variable "prefix" {}

variable "ssh_user" {}

variable "ssh_private_key" {}

variable "servers" {}

variable "network" {}

resource "google_compute_instance" "agent" {
  name         = "${var.prefix}-${count.index}"
  machine_type = "n1-highcpu-8"
  tags         = ["no-ip", "${var.prefix}", "${var.prefix}-${count.index}"]
  zone         = "us-central1-a"
  count        = "${var.servers}"

  disk {
    image = "${var.image}"
    type  = "pd-ssd"
    size  = "30"
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
      "sudo systemctl restart buildkite-agent",
    ]
  }
}
