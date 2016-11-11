###############################
# OpenVPN
###############################

variable "vpn_image" {
}

resource "aws_instance" "vpn" {
    ami                         = "${var.vpn_image}"
    instance_type               = "t2.small"

    tags                        = {
        Name = "vpn"
    }

    subnet_id                   = "${aws_subnet.public.id}"
    associate_public_ip_address = true
    vpc_security_group_ids      = ["${aws_security_group.vpn_tcp_udp_sg.id}"]
    key_name                    = "${var.key_name}"
}

output "vpn_tcp_udp_sg" {
    value = "${aws_security_group.vpn_tcp_udp_sg.id}"
}

resource "aws_security_group" "vpn_tcp_udp_sg" {
    name        = "vpn_ssh"
    description = "Allow VPN connections through TCP/UDP to VPN instance from approved ranges"

    # Allow ping
    # https://github.com/hashicorp/terraform/issues/1313#issuecomment-193631036
    ingress {
        from_port   = 8
        to_port     = 0
        protocol    = "icmp"
        cidr_blocks = ["${var.ip_range}"]
    }

    # Allow SSH connections
    ingress {
        from_port   = 22
        to_port     = 22
        protocol    = "tcp"
        cidr_blocks = ["${var.ip_range}"]
    }

    # Allow TCP for VPN
    ingress {
        from_port   = 1194
        to_port     = 1194
        protocol    = "tcp"
        cidr_blocks = ["${var.ip_range}"]
    }

    # Allow UDP for VPN
    ingress {
        from_port   = 1194
        to_port     = 1194
        protocol    = "udp"
        cidr_blocks = ["${var.ip_range}"]
    }

    egress {
        from_port   = 0
        to_port     = 0
        protocol    = "-1"
        cidr_blocks = ["${var.ip_range}"]
    }

    vpc_id      = "${aws_vpc.default.id}"

    tags {
        Name = "vpn_tcp_udp"
    }
}
