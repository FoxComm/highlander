output "leader_ip" {
    value = "${google_compute_instance.swarm_server.0.network_interface.0.address}"
}

output "ips" {
    value = ["${google_compute_instance.swarm_server.*.network_interface.0.address}"]
}
