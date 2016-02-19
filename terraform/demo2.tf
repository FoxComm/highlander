variable "demo_image" { 
    default = "ubuntu-1510-wily-v20160123"
}

resource "google_compute_instance" "demo2-ashes" { 
    name = "demo2-ashes"
    machine_type = "n1-highcpu-4"
    tags = ["http-server", "https-server", "demo2-ashes"]
    zone = "us-central1-a"

    disk {
        image = "${var.demo_image}"
        type = "pd-ssd"
        size = "30"
    }   

    network_interface {
        network = "default"
        access_config {
        }
    }
}

resource "google_compute_instance" "demo2-backend" { 
    name = "demo2-backend"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "demo2-backend"]
    zone = "us-central1-a"

    disk {
        image = "${var.demo_image}"
        type = "pd-ssd"
        size = "100"
    }   

    network_interface {
        network = "default"
    }
}

resource "google_dns_managed_zone" "demo2" {
    name = "demo2-zone"
    description = "Demo Zone"
    dns_name = "demo2.foxcommerce.com."
}

resource "google_dns_record_set" "demo2" {
    managed_zone = "${google_dns_managed_zone.demo2.name}"
    name = "${google_dns_managed_zone.demo2.dns_name}"
    type = "A"
    ttl = 300
    rrdatas = ["${google_compute_instance.demo2-ashes.network_interface.0.access_config.0.assigned_nat_ip}"]
}
