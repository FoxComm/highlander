variable "ip_range" {
  default = "0.0.0.0/0" # Allow from any ip. We are remote.
}

variable "availability_zones" {
  # No spaces allowed between az names!
  default = ["us-west-2a", "us-west-2b", "us-west-2c"]
}

variable "vpc_cidr" {
  description = "CIDR for the whole VPC"
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR for the Public Subnet"
  default     = "10.0.0.0/24"
}

variable "private_subnet_cidr" {
  description = "CIDR for the Private Subnet"
  default     = "10.0.1.0/24"
}

variable "name" {}

variable "key_name" {}
