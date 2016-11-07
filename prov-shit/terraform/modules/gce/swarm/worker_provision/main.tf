# generic variables
variable "datacenter" {
}
variable "setup" {
}

# resources variables
variable "master_ips" {
    type = "list"
}
variable "worker_ips" {
    type = "list"
}
variable "docker_registry_bucket" {
}
variable "count" {
}

resource "null_resource" "swarm_worker_server_provision" {
    count = "${var.count}"

    connection {
        user = "ubuntu"
        host = "${element(var.worker_ips, count.index)}"
    }

    provisioner "local-exec" {
        command = <<EOF
            ansible-playbook -vvvv -i ${element(var.worker_ips, count.index)}, ansible/bootstrap_swarm_worker.yml \
            --extra-vars @terraform/envs/gce_${var.datacenter}_${var.setup}/params.json \
            --extra-vars '{"zookeepers_ips":${jsonencode(var.master_ips)}}' \
            --extra-vars docker_registry_bucket=${var.docker_registry_bucket}
        EOF
    }
}
