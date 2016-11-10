// provider variables
account_file         = "foxcomm-staging.json"
gce_project          = "foxcomm-staging"
region               = "us-central1"

// generic variables
zone                 = "us-central1-a"
datacenter           = "swarm"
setup                = "production"
network              = "default"
bucket_location      = "us"

// resources variables
master_machine_type  = "n1-standard-1"
master_image         = "swarm-master-161110-204034"
master_disk_size     = "30"
masters_count        = "3"

worker_machine_type  = "n1-standard-1"
worker_image         = "swarm-worker-161110-204752"
worker_disk_size     = "30"
workers_count        = "2"

storage_machine_type = "n1-standard-1"
storage_image        = "swarm-storage-161110-210911"
storage_disk_size    = "60"
