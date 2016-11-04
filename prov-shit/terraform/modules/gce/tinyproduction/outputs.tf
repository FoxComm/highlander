output "amigo_leader" {
    value = "${google_compute_instance.tinyprod-amigo.network_interface.0.address}"
}
