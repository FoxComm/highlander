# Production Environment From Scratch

Still work-in-progress.

Navigation:
* [GCE Project](#gce-project)
* [VPN machine](#vpn-machine)
* [Service machines](#service-machines)

## GCE Project

1. Create a GCE project.
2. Generate your SSH keys and add them to a project (see [Adding and Removing SSH Keys](https://cloud.google.com/compute/docs/instances/adding-removing-ssh-keys)).
3. Download `account.json` service account key file, used by packer & terraform.
4. Create `terraform.tfvars` file containing your keys:

	```
	ssh_user = "pavel"
	ssh_private_key = "/Users/pavel/.ssh/id_rsa"
	```

## VPN machine

**FIXME**: Before doing all this, temporary comment-out all modules inside `terraform/base/gce_vanilla/main.tf`, except `vanilla_vpn` module. Undo changes when you'll get to [Service machines](#service-machines) section.

**FIXME**: Parts 6-11 can be automated.

1. Build core base image and save it's name:

	```
	$ packer build -var-file=packer/envs/vanilla.json packer/base/base.json
	```

2. Create new terraform environment containing variables file, similar too `terraform/envs/terraform.tfvars`. Set proper values for `gce_project`, `account_file` and `vpn_image`.

3. Install dependent terraform modules:

	```
	$ terraform get terraform/base/gce_vanilla
	```

4. Terraform VPN machine:

	```
	$ terraform plan \
		-state terraform/envs/gce_vanilla/terraform.tfstate \
		-var-file terraform/envs/gce_vanilla/terraform.tfvars \
		terraform/base/gce_vanilla
	$ terraform apply \
		-state terraform/envs/gce_vanilla/terraform.tfstate \
		-var-file terraform/envs/gce_vanilla/terraform.tfvars \
		terraform/base/gce_vanilla
	```

5. Create `vanilla_vpn` inventory file and write a created machine VPN there under `vanilla-vpn` (host) section.

6. Fork `bootstrap_prod_small_vpn.yml`, rename the `hosts` section to `vanilla-vpn`

7. Bootstrap OpenVPN service:

	```
	$ ansible-playbook -v -i vanilla_vpn ansible/bootstrap_vanilla_vpn.yml
	```

8. SSH into machine and register VPN accounts:

	```
	$ sudo su
	$ cd /etc/openvpn/easy-rsa
	$ source ./vars
	$ ./build-key username
	```

9. Move certificate files (`ca.crt`, `username.crt`, `username.key`) to temp directory:

	```
	$ mv username* /tmp/
	$ cp ca.crt /tmp/
	$ tar -czf keys.tar.gz *
	```

10. Download it to your machine via `scp` (replace `vanilla_vpn_ip` with actual IP address):

	```
	$ scp pavel@vanilla_vpn_ip:/tmp/keys.tar.gz ./
	```

11. Cleanup temp directory:

	```
	$ rm -rf /tmp/*
	```

## Service machines

Do all the steps while connected to created VPN service.

1. Build base images for backend, frontend and consul servers (can be ran in parallel):

	```
	$ packer build -var-file=packer/envs/vanilla.json packer/base/base_jvm.json
	$ packer build -var-file=packer/envs/vanilla.json packer/base/base_node.json
	$ packer build -var-file=packer/envs/vanilla.json packer/consul/consul_server.json
	```

2. Save base images names above and replace them in `packer/envs/vanilla.json`.

3. Build specific images (can be ran in parallel):

	```
	$ packer build -var-file=packer/envs/vanilla.json packer/vanilla/db.json
	$ packer build -var-file=packer/envs/vanilla.json packer/vanilla/es.json
	$ packer build -var-file=packer/envs/vanilla.json packer/vanilla/es_log.json
	$ packer build -var-file=packer/envs/vanilla.json packer/vanilla/front.json
	$ packer build -var-file=packer/envs/vanilla.json packer/vanilla/greenriver.json
	$ packer build -var-file=packer/envs/vanilla.json packer/vanilla/kafka.json
	$ packer build -var-file=packer/envs/vanilla.json packer/vanilla/phoenix.json
	```

4. Save base images names above and replace them in `terraform/envs/gce_vanilla/terraform.tfvars`.

5. Terraform service machines:

	```
	$ terraform plan \
		-state terraform/envs/gce_vanilla/terraform.tfstate \
		-var-file terraform/envs/gce_vanilla/terraform.tfvars \
		terraform/base/gce_vanilla
	$ terraform apply \
		-state terraform/envs/gce_vanilla/terraform.tfstate \
		-var-file terraform/envs/gce_vanilla/terraform.tfvars \
		terraform/base/gce_vanilla
	```

6. Add a new project ID in `projects.json` and

	```
	$ make build
	```

7. Create `vanilla` executable in root directory used by the bootstrapper:

	```
	#!/bin/bash

	bin/inventory --env vanilla "$@"
	```

8. Bootstrap the initial data:

	```
	$ ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_vanilla.yml
	```
