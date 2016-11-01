output "ips" {
    value = ["${google_compute_instance.swarm_worker_server.*.network_interface.0.address}"]
}
