variable "zone" {
}
variable "datacenter" {
}
variable "network" {
}
variable "image" {
}
variable "count" {
}
variable "ssh_user" {
}
variable "ssh_private_key" {
}

resource "google_compute_instance" "swarm_master_server" {
    name         = "${var.datacenter}-swarm-master-server-${count.index}"
    machine_type = "n1-standard-1"
    tags         = ["ssh", "no-ip", "${var.datacenter}-swarm-master-server-${count.index}", "${var.datacenter}-swarm-master-server", "${var.datacenter}"]
    zone         = "${var.zone}"
    count        = "${var.count}"

    disk {
        image = "${var.image}"
        type  = "pd-ssd"
        size  = "30"
    }

    network_interface {
        network = "${var.network}"
    }

    service_account {
        scopes = ["storage-rw"]
    }

    connection {
        type        = "ssh"
        user        = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }
}
