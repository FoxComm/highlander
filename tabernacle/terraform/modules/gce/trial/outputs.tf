output "amigo_address" {
  value = "${google_compute_instance.trial-amigo.network_interface.0.address}"
}

output "backend_address" {
  value = "${google_compute_instance.trial-backend.network_interface.0.address}"
}

output "frontend_address" {
  value = "${google_compute_instance.trial-frontend.network_interface.0.address}"
}
