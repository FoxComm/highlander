variable "image" {}
variable "datacenter" {}
variable "servers" {}
variable "ssh_user" {} 
variable "ssh_private_key" {} 
variable "network" {} 

resource "google_compute_instance" "consul_server" { 
    name = "${var.datacenter}-consul-server-${count.index}"
    machine_type = "n1-standard-1"
    tags = ["ssh", "no-ip", "${var.datacenter}-consul-server-${count.index}", "${var.datacenter}-consul-server", "${var.datacenter}"]
    zone = "us-central1-a"
    count = "${var.servers}"

    metadata { 
        consul_dc = "${var.datacenter}"
    }

    disk {
        image = "${var.image}"
        type = "pd-ssd"
        size = "30"
    }   

    network_interface {
        network = "${var.network}"
    }

    connection { 
        type = "ssh"
        user = "${var.ssh_user}"
        private_key="${file(var.ssh_private_key)}"
    }

    provisioner "file" {
        source = "terraform/scripts/bootstrap.sh"
        destination = "/tmp/bootstrap.sh"
    }

    provisioner "file" {
        source = "terraform/scripts/consul.sh"
        destination = "/tmp/consul.sh"
    }

    provisioner "remote-exec" {
        inline = [
          "chmod +x /tmp/bootstrap.sh",
          "chmod +x /tmp/consul.sh",
          "/tmp/bootstrap.sh",
          "/tmp/consul.sh ${var.datacenter} ${google_compute_instance.consul_server.0.network_interface.0.address}"
        ]
    }

}
