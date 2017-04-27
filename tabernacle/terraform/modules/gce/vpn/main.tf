variable "image" {}

variable "network" {}

variable "zone" {
  default = "us-central1-a"
}

resource "google_compute_instance" "vpn" {
  name           = "${var.network}-vpn"
  machine_type   = "n1-standard-1"
  tags           = ["ssh", "${var.network}-vpn"]
  zone           = "${var.zone}"
  can_ip_forward = true

  disk {
    image = "${var.image}"
    type  = "pd-ssd"
    size  = "10"
  }

  network_interface {
    network = "${var.network}"

    access_config {}
  }
}

resource "google_compute_firewall" "vpn" {
  name    = "${var.network}-vpn"
  network = "${var.network}"

  allow {
    protocol = "icmp"
  }

  allow {
    protocol = "tcp"
    ports    = ["1194"]
  }

  allow {
    protocol = "udp"
    ports    = ["1194"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["${var.network}-vpn"]
}

resource "google_compute_route" "vpn" {
  name                   = "${var.network}-vpn-route"
  dest_range             = "0.0.0.0/0"
  network                = "${var.network}"
  next_hop_instance      = "${google_compute_instance.vpn.name}"
  next_hop_instance_zone = "${var.zone}"
  priority               = 800
  tags                   = ["no-ip"]
}
