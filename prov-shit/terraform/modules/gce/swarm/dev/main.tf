variable "owner" {
}
variable "zone" {
}
variable "datacenter" {
}
variable "network" {
}
variable "image" {
}
variable "ssh_user" {
}
variable "ssh_private_key" {
}

resource "google_compute_instance" "swarm_dev_server" {
    name         = "${var.datacenter}-swarm-dev-server-${var.owner}"
    machine_type = "n1-standard-1"
    tags         = [
        "ssh",
        "no-ip",
        "${var.datacenter}",
        "${var.datacenter}-swarm-master-server",
        "${var.datacenter}-swarm-master-server-${count.index}"
    ]
    zone         = "${var.zone}"

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

resource "null_resource" "swarm_dev_server_provision" {
    depends_on = ["google_compute_instance.swarm_dev_server"]

    connection {
        user = "ubuntu"
        host = "${google_compute_instance.swarm_dev_server.network_interface.0.address}"
    }

    provisioner "local-exec" {
        command = <<EOF
            ansible-playbook -vvvv -i ${google_compute_instance.swarm_dev_server.network_interface.0.address}, ansible/bootstrap_swarm_development.yml \
            --extra-vars @terraform/devenvs/${var.owner}/params.json \
            --extra-vars '{"zookeepers_ips":["127.0.0.1"]}' \
            --extra-vars mesos_quorum=1 \
            --extra-vars zookeeper_server_id=1
        EOF
    }
}
