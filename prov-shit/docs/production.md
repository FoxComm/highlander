# Production Environment From Scratch

Navigation:
* [Generic operations](#generic-operations)
* [VPN machine](#vpn-machine)
* [Service machines](#service-machines)


## Generic operations

These are expected to be run once, not for each production setup

1. Open foxcommerce-production-shared proejct in GCE
2. Add ssh key if not yet on https://console.cloud.google.com/compute/metadata/sshKeys (see [Adding and Removing SSH Keys](https://cloud.google.com/compute/docs/instances/adding-removing-ssh-keys)).
3. Add service account at https://console.cloud.google.com/iam-admin/serviceaccounts/project
4. Download created service account `account.json` key as `prov-shit/account.json`
4. Ask project owner to provide IAM rights
5. Create `prov-shit/terraform.tfvars` file containing your keys:

	```
	ssh_user = "<username>"
	ssh_private_key = "$HOME/.ssh/id_rsa"
	```

## VPN machine

1. Create terraform base project in `terraform/base/gce_<project>/main.tf` by copying & renaming `terraform/base/gce_vanilla/main.tf` contents. All vanilla mentions are to be renamed into `<project>`.
    
    ```
    # before
    resource "google_compute_network" "vanilla" {
       # Resource parameters
       # ...
    }
    
    # after
    resource "google_compute_network" "<project>" {
       # Resource parameters
       # ...
    }
    ```
    
    It's terraform's internal care about that, but we are reducing error's likelyhood so.

2. Create terraform environment vars in `terraform/envs/gce_<project>/<project>.tfvars` by copying & renaming `terraform/envs/gce_vanilla/vanilla.tfvars`

3. Comment all in `terraform/base/gce_<project>/main.tf`, but network resource and vpn module. This is needed because no way for now to deal with setup dependencies in single run:

    * Creating VPN
    * Gaining access to VPN
    * Creating rest of the machines

4. Install dependent terraform modules:
    
    ```
    $ terraform get terraform/base/gce_<project>
    ```

5. Terraform VPN machine:
    
    ```
    $ terraform plan \
        -state=terraform/envs/gce_<project>/terraform.tfstate \
        -var-file=terraform/envs/gce_<project>/tpg.tfvars \
        terraform/base/gce_<project>

    $ terraform apply \
        -state=terraform/envs/gce_<project>/terraform.tfstate \
        -var-file=terraform/envs/gce_<project>/<project>.tfvars \
        terraform/base/gce_<project>
   ```

6. Uncomment remaining resources: web, ssh, internal. As far as networking is up, they are going to be applied successfully. Run terraforming.

7. Create `bin/envs/<project>_vpn` inventory file and write IP of created machine VPN under `<project>-vpn` (host) section:
    
    ```
    [<project>-vpn]
    xxx.xxx.xxx.xxx
    ```

8. Bootstrap VPN there:
    
    ```
    $ ansible-playbook -v -i bin/envs/<project>_vpn ansible/boo tstrap_vanilla_vpn.yml
    ```

9. Generate OpenVPN credentials (repeat for desired number of credentials):
    
    ```
    $ ansible-playbook -v -i bin/envs/<project>_vpn ansible/bootstrap_openvpn_key.yml
    ```

## Service machines

Do all the steps while connected to created VPN service.

1. Copy `packer/envs/vanilla.json` to `packer/envs/<project>.json`, updating `vanilla` mentions to `<project>` ones

2. Build base image:
    
    ```
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/base/base.json
    ```

3. Update `base_image` variable in `packer/envs/<project>.jso`n with name of created image

4. Build base Mesos image:
    
    ```
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/base/base_mesos.json
    ```

5. Update `base_mesos` variable in `packer/envs/<project>.json` with name of created image

6. Build core base images for JVM and Node.js:
    
    ```
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/base/base_jvm.json
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/base/base_node.json
    ```

7. Update `base_jvm` and `base_node` variables in `packer/envs/<project>.json` with names of created images

8. Build application-specific base images:

    **Stage**
    ```
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/amigos/stage_amigo_server.json
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/vanilla/stage_backend.json
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/vanilla/stage_frontend.json
    ```

    **Production**
    ```
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/amigos/amigo_server.json
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/vanilla/db.json
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/vanilla/es.json
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/vanilla/es_log.json
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/vanilla/front.json
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/vanilla/greenriver.json
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/vanilla/kafka.json
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/vanilla/phoenix.json
    $ packer build -only=google -var-file=packer/envs/<project>.json packer/vanilla/service_worker.json
    ```

9. Update image names in `terraform/envs/gce_tpg/<project>.tfvars`

10. Terraform environment machines
    
    ```
    $ terraform plan \
        -state=terraform/envs/gce_<project>/terraform.tfstate \
        -var-file=terraform/envs/gce_<project>/tpg.tfvars \
        terraform/base/gce_<project>
        
    $ terraform apply \
        -state=terraform/envs/gce_<project>/terraform.tfstate \
        -var-file=terraform/envs/gce_<project>/<project>.tfvars \
       terraform/base/gce_<project>
    ```

11. Add new project ID in `projects.json`
12. Build inventory:
    
    ```
    $ make build
    ```

13. Add `bin/envs/<project>` executable:
    
    ```
    #!/bin/bash
    bin/inventory --env <project> "$@"
    ```

14. Bootstrap the initial data:
    
    ```
    $ ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_vanilla.yml
    ```

15. Bootstrap consul alerts, if necessary (continue with prompts manually):

    ```
    $ ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_consul_alerts.yml
    ```

16. Bootstrap database backups, if necessary (continue with prompts manually):

    ```
    $ ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_db_backup.yml
    ```
