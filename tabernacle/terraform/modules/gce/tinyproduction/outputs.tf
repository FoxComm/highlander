output "amigo_address" {
  value = "${google_compute_instance.tinyprod-amigo.network_interface.0.address}"
}

output "backend_address" {
  value = "${google_compute_instance.tinyprod-backend.network_interface.0.address}"
}

output "frontend_address" {
  value = "${google_compute_instance.tinyprod-frontend.network_interface.0.address}"
}
