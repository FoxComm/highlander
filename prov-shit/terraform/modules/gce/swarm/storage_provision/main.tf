# generic variables
variable "datacenter" {
}
variable "setup" {
}

# resources variables
variable "leader_ip" {
}
variable "storage_ip" {
}

resource "null_resource" "swarm_worker_server_provision" {
    connection {
        user = "ubuntu"
        host = "${var.storage_ip}"
    }

    provisioner "local-exec" {
        command = <<EOF
            ansible-playbook -vvvv -i ${var.storage_ip}, ansible/bootstrap_swarm_storage.yml \
            --extra-vars @terraform/envs/gce_${var.datacenter}_${var.setup}/params.json \
            --extra-vars datacenter="${var.datacenter}" \
            --extra-vars consul_leader="${var.leader_ip}"
        EOF
    }
}
