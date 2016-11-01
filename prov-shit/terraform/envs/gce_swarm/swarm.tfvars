account_file    = "foxcomm-staging.json"
gce_project     = "foxcomm-staging"
region          = "us-central1"
zone            = "us-central1-a"
datacenter      = "swarm"
network         = "vanilla"
bucket_location = "us"

base_image      = "swarm-base-1478000724"
master_image    = "swarm-master-1478003329"
worker_image    = "swarm-worker-1478003522"
masters_count   = 3
workers_count   = 2
