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
	$ export TF_BASE=terraform/base
	$ export TF_ENVS=terraform/envs
	$ terraform plan -state $TF_BASE/gce_vanilla/terraform.tfstate -var-file $TF_BASE/gce_vanilla/terraform.tfvars $TF_BASE/gce_vanilla
	$ terraform apply -state $TF_BASE/gce_vanilla/terraform.tfstate -var-file $TF_BASE/gce_vanilla/terraform.tfvars $TF_BASE/gce_vanilla
	```

5. Create `vanilla_vpn` inventory file and write a created machine VPN there under `vanilla-vpn` (host) section.

7. Bootstrap OpenVPN service:

	```
	$ ansible-playbook -v -i vanilla_vpn ansible/bootstrap_vanilla_vpn.yml
	```

8. Generate OpenVPN credentials (any number you want):

	```
	$ ansible-playbook -v -i vanilla_vpn ansible/bootstrap_openvpn_key.yml
	```

## Service machines

Do all the steps while connected to created VPN service.

1. Build base images for backend, frontend and consul servers (can be ran in parallel):

	```
	$ packer build -var-file=packer/envs/vanilla.json packer/base/base_jvm.json
	$ packer build -var-file=packer/envs/vanilla.json packer/base/base_node.json
	$ packer build -var-file=packer/envs/vanilla.json packer/amigos/amigo_server.json
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
	$ export TF_BASE=terraform/base
	$ export TF_ENVS=terraform/envs
	$ terraform plan -state $TF_BASE/gce_vanilla/terraform.tfstate -var-file $TF_BASE/gce_vanilla/terraform.tfvars $TF_BASE/gce_vanilla
	$ terraform apply -state $TF_BASE/gce_vanilla/terraform.tfstate -var-file $TF_BASE/gce_vanilla/terraform.tfvars $TF_BASE/gce_vanilla
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

9. Bootstrap consul alerts and database backups, if necessary (continue with prompts manually):

	```
	$ ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_consul_alerts.yml
	$ ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_db_backup.yml
	```
