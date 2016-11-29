# provider variables
account_file          = "aws_target_stage"
region                = "us-west-2"

# generic variables
zone                  = "us-west-2a"
datacenter            = "fc"
setup                 = "stage"
key_name              = "fc-stage"
policy_file           = "terraform/policy/stage.json"

# network variables
vpc_cidr_block        = "10.0.0.0/24"

# resources variables
amigo_image           = "___"
amigo_machine_type    = "t2.medium"
frontend_image        = "___"
frontend_machine_type = "m3.xlarge"
backend_image         = "___"
backend_machine_type  = "r3.xlarge"

# prepared resources
## VPC
#vpc-3f8e7858 10.0.0.0/16
#
## subnets
#fc-stage-public-001 subnet-ba2c3ede
#fc-stage-public-002 subnet-76d9ed00
#fc-stage-public-003 subnet-79046621
#fc-stage-private-001 subnet-bb2c3edf
#fc-stage-private-002 subnet-77d9ed01
#fc-stage-private-003 subnet-7a046622
#
## internet gateway
#fc-stage igw-742da210
#
## routing table
#fc-stage rtb-345fe353 (for 3 private subnets)
#
## elastic ip
#35.161.157.61 eipalloc-7474e413 eni-6f862b11 10.0.0.235
#
## network interfaces
#eni-6f862b11 subnet-ba2c3ede 35.161.157.61 10.0.0.235 (public-001)
#
## nat gateway
#nat-0981aca23c3ce4964 35.161.157.61 10.0.0.235
#
## security groups
#sg-9e0472e7 default           default VPC security group
#sg-8b0472f2 fc-stage-instance fc instance security group
