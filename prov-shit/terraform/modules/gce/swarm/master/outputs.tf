output "leader" {
    value = "${google_compute_instance.swarm_master_server.0.network_interface.0.address}"
}
