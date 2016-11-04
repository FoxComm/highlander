# generic variables
variable "datacenter" {
}

# resources variables
variable "master_ips" {
    type = "list"
}
variable "count" {
}

resource "null_resource" "swarm_master_server_provision" {
    count = "${var.count}"

    connection {
        user = "ubuntu"
        host = "${element(var.master_ips, count.index)}"
    }

    provisioner "local-exec" {
        command = <<EOF
            ansible-playbook -vvvv -i ${element(var.master_ips, count.index)}, ansible/bootstrap_swarm_master.yml \
            --extra-vars @terraform/envs/gce_${var.datacenter}/params.json \
            --extra-vars '{"zookeepers_ips":${jsonencode(var.master_ips)}}' \
            --extra-vars mesos_quorum=${(var.count + (var.count % 2))/2} \
            --extra-vars zookeeper_server_id=${count.index + 1}
        EOF
    }
}
