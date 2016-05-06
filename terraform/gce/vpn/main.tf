variable "image" {}
variable "network" {} 
variable "zone" {
    default = "us-central1-a"
}

resource "google_compute_instance" "vpn" { 
    name = "${var.network}-vpn"
    machine_type = "n1-standard-1"
    tags = ["${var.network}-vpn"]
    zone = "${var.zone}"
    can_ip_forward = true

    disk {
        image = "${var.image}"
        type = "pd-ssd"
        size = "10"
    }   

    network_interface {
        network = "${var.network}"
        access_config {
        }
    }
}
