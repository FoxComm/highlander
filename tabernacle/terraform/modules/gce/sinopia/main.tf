variable "ssh_user" {}

variable "ssh_private_key" {}

variable "dnsimple_account" {}

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
# VM Instance
##############################################
resource "google_compute_instance" "sinopia" {
  name         = "${var.datacenter}-sinopia"
  machine_type = "${var.instance_type}"
  tags         = ["ssh", "sinopia", "http-server", "https-server"]
  zone         = "us-central1-a"

  disk {
    image = "${var.image}"
    type  = "pd-ssd"
    size  = "30"
  }

  network_interface {
    network = "${var.network}"

    access_config {}
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
  value  = "${google_compute_instance.sinopia.network_interface.0.access_config.0.assigned_nat_ip}"
  type   = "A"
  ttl    = 3600
}
