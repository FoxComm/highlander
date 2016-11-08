// provider variables
account_file        = "foxcomm-staging.json"
gce_project         = "foxcomm-staging"
region              = "us-central1"

// generic variables
zone                = "us-central1-a"
datacenter          = "swarm"
setup               = "staging"
network             = "default"
bucket_location     = "us"

// resources variables
master_machine_type = "n1-standard-1"
master_image        = "swarm-master-161108-133616"
master_disk_size    = "30"
masters_count       = "1"
worker_machine_type = "n1-standard-1"
worker_image        = "swarm-worker-161108-134311"
worker_disk_size    = "30"
workers_count       = "2"
