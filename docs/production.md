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

## VPN machine

1. Build core base image:

	```
	$ packer build -var-file=packer/envs/vanilla.json packer/base/base.json
	```

2. *Fork Terraform configurations from both `terraform/envs/gce_prod_small` and `terraform/gce/prodsmall_stack`. Remove state file (`.tfstate`) and do all necessary replacements (e.g. `prodsmall` -> `vanilla`). Replace `vpn_image` variable with the generated base image above.*

3. Install VPN module for configuration:

	```
	$ terraform get terraform/envs/gce_vanilla
	```

4. Plan and apply infrastructure changes:

	```
	$ terraform plan -state terraform/envs/gce_vanilla/terraform.tfstate terraform/envs/gce_vanilla
	$ terraform apply -state terraform/envs/gce_vanilla/terraform.tfstate terraform/envs/gce_vanilla
	```

5. Create `vanilla_vpn` inventory file and write a created machine VPN there under `vanilla-vpn` (host) section.

6. *Fork `bootstrap_prod_small_vpn.yml`, rename the `hosts` section to `vanilla-vpn`*

7. Provision an OpenVPN role:

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

4. Save base images names above and replace them in `terraform/envs/gce_vanilla/main.tf`.

5. Terraform the machines:

	```
	$ terraform plan -state terraform/envs/gce_vanilla/terraform.tfstate terraform/envs/gce_vanilla
	$ terraform apply -state terraform/envs/gce_vanilla/terraform.tfstate terraform/envs/gce_vanilla
	```

6. Add a new project ID in `bin/env/projects.json` and

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
