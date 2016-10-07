
output "kakfa_address" { 
    value = "${google_compute_instance.kafka.network_interface.0.address}"
}
