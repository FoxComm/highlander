# Terraform Guide

This guide describes how to add new machines to existing infrastructure in Google Compute Engine safely.

Before doing all this, you'll need to have an access to `foxcomm-staging` project in GCE. Ask [@jmataya](https://github.com/jmataya) to grant permissions.

## Preparation

1. Download pre-built terraform [binary](https://www.terraform.io/downloads.html).

2. Download your GCE authentication JSON File ([complete guide](https://www.terraform.io/docs/providers/google/), see `Authentication JSON File` at the bottom) and put it in the root of this repository.

3. Generate SSH key-pair on your machine and add it to project metadata - [complete guide](https://cloud.google.com/compute/docs/instances/connecting-to-instance#generatesshkeypair).

4. Create `terraform.tfvars` variables file containing generated keypair information in the root of this repository:

	```
	ssh_user = "maxim"
	ssh_private_key = "/home/maxim/.ssh/id_rsa"
	```

## Terraforming

TBD