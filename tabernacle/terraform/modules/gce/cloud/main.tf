variable "agent_image" {}

variable "instance_name" {}

variable "dns_record" {}

variable "ssh_user" {}

variable "ssh_private_key" {}

variable "dnsimple_token" {}

variable "dnsimple_email" {}

resource "google_compute_instance" "agent" {
  name         = "${var.instance_name}"
  machine_type = "n1-standard-4"
  tags         = ["no-ip", "${var.instance_name}"]
  zone         = "us-central1-a"

  disk {
    image = "${var.agent_image}"
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
      "sudo systemctl restart buildkite-agent",
    ]
  }
}

##############################################
# Setup DNS
##############################################
provider "dnsimple" {
  token = "${var.dnsimple_token}"
  email = "${var.dnsimple_email}"
}

resource "dnsimple_record" "agent-dns-record" {
  domain = "foxcommerce.com"
  name   = "${var.dns_record}"
  value  = "${google_compute_instance.agent.0.network_interface.0.address}"
  type   = "A"
  ttl    = 3600
}
