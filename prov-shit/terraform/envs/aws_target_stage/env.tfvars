# provider variables
access_key            = "AKIAIOVFZKMXSV4TNGSQ"
secret_key            = "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"
region                = "us-west-2"

# generic variables
zone                  = "us-west-2a"
datacenter            = "fc-target"
setup                 = "stage"
key_name              = "tgt"
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
