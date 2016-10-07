
output "leader" { 
    value = "${aws_instance.amigo_server_0.private_ip}"
}
