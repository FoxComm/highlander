variable "gatling_image" { 
    default = "ubuntu-1510-wily-v20160123"
}

resource "google_compute_instance" "gatling-gun"{ 
    name = "gatling-gun"
    machine_type = "n1-standard-4"
    tags = ["no-ip", "gatling-gun"]
    zone = "us-central1-a"

    disk {
        image = "${var.gatling_image}"
        type = "pd-ssd"
        size = "30"
    }   

    network_interface {
        network = "default"
    }
}

resource "google_compute_instance" "gatling-ashes" { 
    name = "gatling-ashes"
    machine_type = "n1-highcpu-4"
    tags = ["no-ip", "gatling-ashes"]
    zone = "us-central1-a"

    disk {
        image = "${var.gatling_image}"
        type = "pd-ssd"
        size = "30"
    }   

    network_interface {
        network = "default"
    }

    provisioner "local-exec" {
        command = <<EOF
        ansible-playbook -T 30 -i ./staging ansible/gatling_ashes.yml
EOF
    }
}

resource "google_compute_instance" "gatling-backend" { 
    name = "gatling-backend"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "gatling-backend"]
    zone = "us-central1-a"

    disk {
        image = "${var.gatling_image}"
        type = "pd-ssd"
        size = "100"
    }   

    network_interface {
        network = "default"
    }
    provisioner "local-exec" {
        command = <<EOF
        ansible-playbook -T 30 -i ./staging ansible/gatling_backend.yml
EOF
    }
}
