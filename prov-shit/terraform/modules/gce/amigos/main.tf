variable "image" {}
variable "datacenter" {}
variable "servers" {}
variable "ssh_user" {}
variable "ssh_private_key" {}
variable "network" {}

resource "google_compute_instance" "amigo_server" {
    name = "${var.datacenter}-amigo-server-${count.index}"
    machine_type = "n1-standard-2"
    tags = ["ssh", "no-ip", "${var.datacenter}-amigo-server-${count.index}", "${var.datacenter}-amigo-server", "${var.datacenter}"]
    zone = "us-central1-a"
    count = "${var.servers}"

    metadata {
        amigo_dc = "${var.datacenter}"
    }

    disk {
        image = "${var.image}"
        type = "pd-ssd"
        size = "30"
    }

    network_interface {
        network = "${var.network}"
    }

    service_account {
        scopes = ["storage-rw"]
    }

    connection {
        type = "ssh"
        user = "${var.ssh_user}"
        private_key = "${file(var.ssh_private_key)}"
    }

    provisioner "remote-exec" {
        inline = [
          "/usr/local/bin/bootstrap.sh",
          "/usr/local/bin/bootstrap_consul.sh ${var.datacenter} ${google_compute_instance.amigo_server.0.network_interface.0.address}",
          "sudo su -c 'echo ${count.index + 1} > /var/lib/zookeeper/myid'"
        ]
    }
}
