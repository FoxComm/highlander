variable "zone" {
}
variable "datacenter" {
}
variable "network" {
}
variable "inventory" {
}
variable "docker_registry_bucket" {
}
variable "master_ips" {
    type = "list"
}
variable "worker_ips" {
    type = "list"
}
variable "image" {
}
variable "count" {
}
variable "ssh_user" {
}
variable "ssh_private_key" {
}

resource "google_compute_instance" "swarm_worker_server" {
    name         = "${var.datacenter}-swarm-worker-server-${count.index}"
    machine_type = "n1-standard-1"
    tags         = [
        "ssh",
        "no-ip",
        "${var.datacenter}",
        "${var.datacenter}-swarm-worker-server",
        "${var.datacenter}-swarm-worker-server-${count.index}"
    ]
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

resource "null_resource" "swarm_worker_server_provision" {
    depends_on = ["google_compute_instance.swarm_worker_server"]

    count      = "${var.count}"

    connection {
        user = "ubuntu"
        host = "${element(google_compute_instance.swarm_worker_server.*.network_interface.0.address, count.index)}"
    }

    provisioner "local-exec" {
        command = <<EOF
            ansible-playbook -vvvv -i ${join(",",var.worker_ips)}, ansible/bootstrap_swarm_worker.yml \
            --extra-vars @terraform/envs/gce_${var.datacenter}/params.json \
            --extra-vars '{"zookeepers_ips":${jsonencode(var.master_ips)}}' \
            --extra-vars docker_registry_bucket=${var.docker_registry_bucket}
        EOF
    }
}
