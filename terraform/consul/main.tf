variable "image" {}
variable "datacenter" {}
variable "servers" {}
variable "ssh_user" {} 
variable "ssh_private_key" {} 

resource "google_compute_instance" "consul_server" { 
    name = "${var.datacenter}-consul-server-${count.index}"
    machine_type = "n1-highmem-4"
    tags = ["no-ip", "${var.datacenter}-consul-server-${count.index}", "${var.datacenter}-consol-server", "${var.datacenter}"]
    zone = "us-central1-a"
    count = "${var.servers}"

    metadata { 
        consul_dc = "${var.datacenter}"
    }

    disk {
        image = "${var.image}"
        type = "pd-ssd"
        size = "50"
    }   

    network_interface {
        network = "default"
    }

    connection { 
        type = "ssh"
        user = "${var.ssh_user}"
        private_key="${file(var.ssh_private_key)}"
    }

    provisioner "file" {
        source = "terraform/scripts/bootsrap.sh"
        destination = "/tmp/bootsrap.sh"
    }

    provisioner "remote-exec" {
        inline = [
          "chmod +x /tmp/bootsrap.sh",
          "/tmp/bootsrap.sh",
          "echo 'CONSUL_DC=${var.datacenter}' >> /etc/consul.d/env",
          "echo 'CONSUL_SERVER=${google_compute_instance.consul_server.0.private_dns}' >> /etc/consul.d/env",
        ]
    }
}
