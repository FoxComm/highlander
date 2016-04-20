
variable "ssh_user" {} 
variable "ssh_private_key" {} 

module "agent-1" {
    source = "./agent"
    prefix = "buildkite-agent-1"
    queue = "core"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "agent-2" {
    source = "./agent"
    prefix = "buildkite-agent-2"
    queue = "core"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "agent-3" {
    source = "./agent"
    prefix = "buildkite-agent-3"
    queue = "core"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "agent-4" {
    source = "./agent"
    prefix = "buildkite-agent-4"
    queue = "core"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}
