// provider variables
account_file         = "foxcomm-staging.json"
gce_project          = "foxcomm-staging"
region               = "us-central1"

// generic variables
zone                 = "us-central1-a"
datacenter           = "swarm"
setup                = "staging"
network              = "default"
bucket_location      = "us"

// resources variables
master_machine_type  = "n1-standard-1"
master_image         = "swarm-master-161115-150254"
master_disk_size     = "30"
masters_count        = "1"

worker_machine_type  = "n1-standard-1"
worker_image         = "swarm-worker-161115-150259"
worker_disk_size     = "30"
workers_count        = "2"

storage_machine_type = "n1-standard-1"
storage_image        = "swarm-storage-161115-150303"
storage_disk_size    = "60"
