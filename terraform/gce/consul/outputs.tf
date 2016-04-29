
output "leader" { 
    value = "${google_compute_instance.consul_server.0.network_interface.0.address}"
}
