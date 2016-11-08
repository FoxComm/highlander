# generic variables
variable "datacenter" {
}
variable "setup" {
}

# resources variables
variable "masters_ips" {
    type = "list"
}
variable "leader_ip" {
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
            --extra-vars '{"masters_ips":${jsonencode(var.masters_ips)}}' \
            --extra-vars datacenter="${var.datacenter}" \
            --extra-vars consul_leader="${var.leader_ip}" \
            --extra-vars docker_registry_bucket="${var.docker_registry_bucket}"
        EOF
    }
}
