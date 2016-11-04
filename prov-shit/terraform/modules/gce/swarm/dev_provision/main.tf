# resources variables
variable "host_address" {
}

# user variables
variable "owner" {
}

resource "null_resource" "swarm_dev_server_provision" {

    connection {
        user = "ubuntu"
        host = "${var.host_address}"
    }

    provisioner "local-exec" {
        command = <<EOF
            ansible-playbook -vvvv -i ${var.host_address}, ansible/bootstrap_swarm_development.yml \
            --extra-vars @terraform/devenvs/${var.owner}/params.json \
            --extra-vars '{"zookeepers_ips":["127.0.0.1"]}' \
            --extra-vars mesos_quorum=1 \
            --extra-vars zookeeper_server_id=1
        EOF
    }
}
