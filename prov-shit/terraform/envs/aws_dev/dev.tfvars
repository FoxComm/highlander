region = "us-west-2"
key_name = "goldfish"

public_subnet_id = "subnet-9794cdf3"
private_subnet_id = "subnet-9094cdf4"
public_security_groups = ["sg-449b8922", "sg-60051106", "sg-7905111f", "sg-a021d3d9", "sg-0f38ca76"]
private_security_groups = ["sg-60051106", "sg-7905111f", "sg-a021d3d9", "sg-0f38ca76"]

amigo_image = "ami-2f862a4f"
amigo_instance_type = "m4.large"
amigo_volume_size = "20"

frontend_image = "ami-d1a509b1"
frontend_instance_type = "m4.large"
frontend_volume_size = "20"

backend_image = "ami-73a20e13"
backend_instance_type = "m4.xlarge"
backend_volume_size = "60"

amigo_leader_datacenter = "dev"
amigo_leader_image = "ami-84bd11e4"
