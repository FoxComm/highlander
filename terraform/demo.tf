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
