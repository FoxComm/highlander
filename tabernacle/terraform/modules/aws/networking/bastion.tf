###############################

# Bastion Security Group

###############################

resource "aws_security_group" "bastion_ssh_sg" {
  name        = "bastion_ssh"
  description = "Allow SSH to Bastion host from approved ranges"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["${var.ip_range}"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  vpc_id = "${aws_vpc.default.id}"

  tags {
    Name = "bastion_ssh"
  }
}

output "bastion_ssh_sg_id" {
  value = "${aws_security_group.bastion_ssh_sg.id}"
}

resource "aws_security_group" "ssh_from_bastion_sg" {
  name        = "ssh_from_bastion"
  description = "Allow SSH from Bastion host(s)"

  ingress {
    from_port = 22
    to_port   = 22
    protocol  = "tcp"

    security_groups = [
      "${aws_security_group.bastion_ssh_sg.id}",
      "${aws_security_group.nat.id}",
    ]
  }

  vpc_id = "${aws_vpc.default.id}"

  tags {
    Name = "ssh_from_bastion"
  }
}

output "ssh_from_bastion_sg_id" {
  value = "${aws_security_group.ssh_from_bastion_sg.id}"
}

###############################

# Bastion Instance

###############################

resource "aws_instance" "bastion" {
  ami           = "ami-9abea4fb" # Ubuntu Server 14.04 LTS (HVM)
  instance_type = "t2.small"

  tags = {
    Name = "bastion"
  }

  subnet_id                   = "${aws_subnet.public.id}"
  associate_public_ip_address = true
  vpc_security_group_ids      = ["${aws_security_group.bastion_ssh_sg.id}"]
  key_name                    = "${var.key_name}"
}

resource "aws_eip" "bastion" {
  instance = "${aws_instance.bastion.id}"
  vpc      = true
}
