region                = "us-west-2"
aws_key_name          = "tgt"
public_subnet_cidr    = "10.0.8.0/23"
private_subnet_cidr   = "10.0.0.0/21"
vpn_image             = "ami-a6df03c6"

base_image            = "ami-500ed230"
amigo_cluster_image   = "ami-300ed250"
kafka_image           = "ami-330ed253"
db_image              = "ami-380ad658"
es_image              = "ami-5d08d43d"
log_image             = "ami-d40ed2b4"
phoenix_image         = "vanilla-phoenix-1472852437"
greenriver_image      = "vanilla-green-1472854084"
front_image           = "vanilla-front-1471026302"
front_workers         = "1"
service_worker_image  = "ami-3b0ad65b"
service_workers       = "1"

stage_backend_image   = "vanilla-stage-backend-1472678787"
stage_frontend_image  = "vanilla-stage-frontend-1473791299"
stage_amigo_image     = "amigo-server-1470351008"