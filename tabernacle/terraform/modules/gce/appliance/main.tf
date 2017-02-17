variable "ssh_user" {}

variable "ssh_private_key" {}

variable "dnsimple_email" {}

variable "dnsimple_token" {}

variable "appliance_image" {}

variable "datacenter" {}

variable "dns_record" {}

variable "consul_leader" {}

resource "google_compute_instance" "appliance" {
  name         = "${var.datacenter}-consul-server"
  machine_type = "n1-standard-4"
  tags         = ["no-ip", "${var.datacenter}"]
  zone         = "us-central1-a"

  service_account {
    scopes = ["storage-rw"]
  }

  disk {
    image = "${var.appliance_image}"
    type  = "pd-ssd"
    size  = "40"
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

resource "dnsimple_record" "frontend-dns-record" {
  domain = "foxcommerce.com"
  name   = "feature-branch-returns"
  value  = "${google_compute_instance.appliance.0.network_interface.0.address}"
  type   = "A"
  ttl    = 3600
}
