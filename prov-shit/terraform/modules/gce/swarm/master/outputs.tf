output "leader" {
    value = "${google_compute_instance.swarm_master_server.0.network_interface.0.address}"
}

output "ips" {
    value = ["${google_compute_instance.swarm_master_server.*.network_interface.0.address}"]
}
