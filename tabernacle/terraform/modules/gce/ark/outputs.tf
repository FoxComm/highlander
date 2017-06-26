output "amigo_address" {
  value = "${google_compute_instance.ark-amigo-0.network_interface.0.address}"
}

output "frontend_address" {
  value = "${google_compute_instance.ark-frontend.network_interface.0.address}"
}
