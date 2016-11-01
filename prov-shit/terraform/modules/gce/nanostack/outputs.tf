
output "consul_address" {
    value = "${google_compute_instance.amigo.network_interface.0.address}"
}
