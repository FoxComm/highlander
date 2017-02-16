###############################
# Setup VPC
###############################

resource "aws_vpc" "default" {
  cidr_block           = "${var.vpc_cidr}"
  enable_dns_hostnames = true

  tags {
    name = "${var.name}"
  }
}

resource "aws_internet_gateway" "default" {
  vpc_id = "${aws_vpc.default.id}"

  tags {
    Name = "terraform_igw"
  }
}

output "vpc_id" {
  value = "${aws_vpc.default.id}"
}

###############################
# Public Subnet
###############################

resource "aws_subnet" "public" {
  vpc_id            = "${aws_vpc.default.id}"
  cidr_block        = "${var.public_subnet_cidr}"
  availability_zone = "${element(var.availability_zones, 0)}"

  tags {
    Name = "terraform_public_subnet"
  }
}

output "public_subnet_id" {
  value = "${aws_subnet.public.id}"
}

resource "aws_route_table" "public" {
  vpc_id = "${aws_vpc.default.id}"

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = "${aws_internet_gateway.default.id}"
  }

  tags {
    Name = "terraform_public_subnet_route_table"
  }
}

resource "aws_route_table_association" "public" {
  subnet_id      = "${aws_subnet.public.id}"
  route_table_id = "${aws_route_table.public.id}"
}

###############################
# Private Subnet
###############################

resource "aws_subnet" "private" {
  vpc_id            = "${aws_vpc.default.id}"
  cidr_block        = "${var.private_subnet_cidr}"
  availability_zone = "${element(var.availability_zones, 0)}"

  tags {
    Name = "terraform_private_subnet"
  }
}

output "private_subnet_id" {
  value = "${aws_subnet.private.id}"
}

resource "aws_route_table" "private" {
  vpc_id = "${aws_vpc.default.id}"

  route {
    cidr_block  = "0.0.0.0/0"
    instance_id = "${aws_instance.nat.id}"
  }

  tags {
    Name = "terraform_private_subnet_route_table"
  }
}

resource "aws_route_table_association" "private" {
  subnet_id      = "${aws_subnet.private.id}"
  route_table_id = "${aws_route_table.private.id}"
}
