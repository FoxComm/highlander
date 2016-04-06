variable "usertest_image" { 
    default = "ubuntu-1510-wily-v20160123"
    prefix = "usertest1"
}

variable "prefix" {} 

resource "google_compute_instance" "ashes" { 
    name = "${var.prefix}-ashes"
    machine_type = "n1-highcpu-8"
    tags = ["http-server", "https-server", "${var.prefix}-ashes"]
    zone = "us-central1-a"

    disk {
        image = "${var.usertest_image}"
        type = "pd-ssd"
        size = "30"
    }   

    network_interface {
        network = "default"
        access_config {
        }
    }
}

resource "google_compute_instance" "backend" { 
    name = "${var.prefix}-backend"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "${var.prefix}-backend"]
    zone = "us-central1-a"

    disk {
        image = "${var.usertest_image}"
        type = "pd-ssd"
        size = "100"
    }   

    network_interface {
        network = "default"
    }
}
