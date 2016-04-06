variable "demo_image" { 
    default = "ubuntu-1510-wily-v20160123"
}

resource "google_compute_instance" "demo-ashes" { 
    name = "demo-ashes"
    machine_type = "n1-highcpu-8"
    tags = ["http-server", "https-server", "demo-ashes"]
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

resource "google_compute_instance" "demo-backend" { 
    name = "demo-backend"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "demo-backend"]
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

resource "google_dns_managed_zone" "demo" {
    name = "demo-zone"
    description = "Demo Zone"
    dns_name = "demo.foxcommerce.com."
}

resource "google_dns_record_set" "demo" {
    managed_zone = "${google_dns_managed_zone.demo.name}"
    name = "${google_dns_managed_zone.demo.dns_name}"
    type = "A"
    ttl = 300
    rrdatas = ["${google_compute_instance.demo-ashes.network_interface.0.access_config.0.assigned_nat_ip}"]
}
