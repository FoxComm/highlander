account_file    = "foxcomm-staging.json"
gce_project     = "foxcomm-staging"
region          = "us-central1"
zone            = "us-central1-a"
datacenter      = "swarm"
network         = "default"
bucket_location = "us"

base_image      = "swarm-base-161103-143023"
master_image    = "swarm-master-161103-143828"
worker_image    = "swarm-worker-161103-143835"

masters_count   = "1"
workers_count   = "2"
