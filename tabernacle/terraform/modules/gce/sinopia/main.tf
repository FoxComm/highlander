variable "ssh_user" {}

variable "ssh_private_key" {}

variable "dnsimple_email" {}

variable "dnsimple_token" {}

variable "image" {}

variable "network" {}

variable "datacenter" {}

variable "amigo_leader" {}

variable "domain" {}

variable "subdomain" {}

variable "instance_type" {
  default = "n1-standard-2"
}

##############################################
# Firewall Rule
##############################################
resource "google_compute_firewall" "sinopia" {
  name    = "${var.network}-sinopia"
  network = "${var.network}"

  allow {
    protocol = "icmp"
  }

  allow {
    protocol = "tcp"
    ports    = ["4873"]
  }

  source_ranges = ["0.0.0.0/0"]
}

##############################################
# VM Instance
##############################################
resource "google_compute_instance" "sinopia" {
  name         = "${var.datacenter}-sinopia"
  machine_type = "${var.instance_type}"
  tags         = ["ssh", "no-ip", "${var.network}-sinopia"]
  zone         = "us-central1-a"

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
      "/usr/local/bin/bootstrap.sh",
      "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${var.amigo_leader}",
    ]
  }
}

##############################################
# DNS Records
##############################################
resource "dnsimple_record" "sinopia-dns-record" {
  domain = "${var.domain}"
  name   = "${var.subdomain}"
  value  = "${google_compute_instance.sinopia.network_interface.0.address}"
  type   = "A"
  ttl    = 3600
}
