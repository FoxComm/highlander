
output "amigo_address" { 
    value = "${aws_instance.tinyprod-amigo.private_ip}"
}
