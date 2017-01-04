output "consul_address" {
  value = "${google_compute_instance.tiny-consul.network_interface.0.address}"
}
