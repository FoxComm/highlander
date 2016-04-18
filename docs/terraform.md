# Terraform Guide

This guide describes how to add new machines to existing infrastructure in Google Compute Engine safely.

Before doing all this, you'll need to have an access to [foxcomm-staging](https://console.cloud.google.com/compute/instances?project=foxcomm-staging) project in GCE. Ask [@jmataya](https://github.com/jmataya) to grant permissions.

## Preparation

1. Install Terraform - [complete guide](https://www.terraform.io/intro/getting-started/install.html).

2. Download your GCE authentication JSON File ([complete guide](https://www.terraform.io/docs/providers/google/), see `Authentication JSON File` at the bottom) and put it in the root of this repository.

3. Generate SSH key-pair on your machine and add it to project metadata - [complete guide](https://cloud.google.com/compute/docs/instances/connecting-to-instance#generatesshkeypair).

4. Create `terraform.tfvars` variables file containing generated keypair information in the root of this repository:

	```
	ssh_user = "maxim"
	ssh_private_key = "/home/maxim/.ssh/id_rsa"
	```

## Terraforming

1. Create terraform configuration - simplest example with provisioning script can be found in pull request [#98](https://github.com/FoxComm/prov-shit/pull/98)

2. After creating configuration, run `terraform plan terraform/`

	**Note**: make sure you run the `terraform` command in the `prov-shit` directory, **NOT** the `prov-shit/terraform` directory - because statefile is located in the root directory.

3. Command above will tell you exactly which machines it will create, change or destroy. **Please carefully review your changes!**

4. Apply your changes by running `terraform apply terraform/`, it will modify statefile `terraform.tfstate`. You should commit it and push it to the repository.

	**Note**: since statefile must be shared with everyone, wonly one person should run the `terraform` commands at a time.

## Links

* [Terraform Documentation](https://www.terraform.io/docs/index.html)