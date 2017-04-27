output "leader" {
  value = "${aws_instance.amigo_leader.0.private_ip}"
}
