output "consul_address" {
  value = "${google_compute_instance.tiny-consul.network_interface.0.address}"
}

output "frontend_address" {
  value = "${google_compute_instance.tiny-frontend.network_interface.0.address}"
}
