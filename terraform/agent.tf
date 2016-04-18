
variable "ssh_user" {} 
variable "ssh_private_key" {} 

module "agent-core" {
    source = "./agent"
    prefix = "buildkite-agent"
    queue  = "core"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "agent-ashes" {
    source = "./agent"
    prefix = "buildkite-agent"
    queue  = "ashes_stage"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"
}

module "agent-stage" {
    source = "./agent"
    prefix = "buildkite-agent"
    queue  = "stage_master"
    ssh_user = "${var.ssh_user}"
    ssh_private_key = "${var.ssh_private_key}"	
}