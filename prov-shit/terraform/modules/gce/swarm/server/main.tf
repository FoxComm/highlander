# generic variables
variable "zone" {
}
variable "datacenter" {
}
variable "setup" {
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
    default = ""
}

# provisioner variables
variable "ssh_user" {
}
variable "ssh_private_key" {
}

resource "google_compute_instance" "swarm_server" {
    name         = "${join("-", compact(list(var.datacenter, var.setup, var.machine_role, var.owner, lookup(map("1",""), format("%s", var.count), format("%d", count.index)))))}"

    count        = "${var.count}"

    zone         = "${var.zone}"

    machine_type = "${var.machine_type}"

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

    tags         = [
        "ssh",
        "no-ip",
        "${var.datacenter}-${var.setup}",
        "${var.datacenter}-${var.setup}-${var.machine_role}",
        "${var.datacenter}-${var.setup}-${var.machine_role}-${count.index}"
    ]
}
