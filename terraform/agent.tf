
variable "ssh_user" {} 
variable "ssh_private_key" {} 

module "agent-core" {
    source = "./agent"
    prefix = "buildkite-agent"
    queue  = "core"
    group_size = 2
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "agent-ashes" {
    source = "./agent"
    prefix = "buildkite-agent"
    queue  = "ashes_stage"
    group_size = 2
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
