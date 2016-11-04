# generic variables
variable "zone" {
}
variable "datacenter" {
}
variable "network" {
}

# resources variables
variable "machine_role" {
}
variable "machine_type" {
}
variable "image" {
}
variable "disk_size" {
}
variable "count" {
}

# user variables
variable "owner" {
}

# provisioner variables
variable "ssh_user" {
}
variable "ssh_private_key" {
}

resource "google_compute_instance" "swarm_server" {
    name         = "${var.datacenter}-swarm-${var.machine_role}-${var.owner}"
    machine_type = "${var.machine_type}"
    tags         = [
        "ssh",
        "no-ip",
        "${var.datacenter}",
        "${var.datacenter}-swarm-${var.machine_role}",
        "${var.datacenter}-swarm-${var.machine_role}-${count.index}"
    ]
    zone         = "${var.zone}"

    disk {
        image = "${var.image}"
        type  = "pd-ssd"
        size  = "${var.disk_size}"
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
