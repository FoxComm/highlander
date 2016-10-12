# Production Environment From Scratch

Navigation:
* [Preparation](#preparation)
* [VPN machine](#vpn-machine)
* [Service machines](#service-machines)
* [Mesosphere](#mesosphere)

## Preparation

These are expected to be run once, not for each production setup

1. Open [foxcommerce-production-shared](https://console.cloud.google.com/compute/instances?project=foxcomm-production-shared) project in GCE.
2. Add SSH key if not yet on [SSH Keys](https://console.cloud.google.com/compute/metadata/sshKeys) page.
3. Add service account at [Service Accounts](https://console.cloud.google.com/iam-admin/serviceaccounts/project) page.
4. Download created service account file to `prov-shit/account.json`
5. Download common service account to `ansible/roles/base/secret_keys/files/services/foxcomm-production-shared.json`
6. Ask project owner to provide IAM rights.
7. Create `prov-shit/terraform.tfvars` file containing your keys:

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

6. Uncomment remaining resources: `web`, `ssh`, `internal`. As far as networking is up, they are going to be applied successfully. Run terraforming again.

7. Create `bin/envs/<project>_vpn` inventory file and write IP of created machine VPN under `<project>-vpn` (host) section:

    ```
    [<project>-vpn]
    xxx.xxx.xxx.xxx
    ```

8. Bootstrap VPN there:

    ```
    $ ansible-playbook -v -i bin/envs/<project>_vpn ansible/playbook/bootstrap/vanilla_vpn.yml
    ```

9. Generate OpenVPN credentials (repeat for desired number of credentials):

    ```
    $ ansible-playbook -v -i bin/envs/<project>_vpn ansible/playbook/bootstrap/openvpn_key.yml
    ```

## Service machines

Do all the steps while connected to created VPN service.

1. Copy `packer/envs/vanilla/*` to `packer/envs/<project>/*`.

2. Build base image:

    ```
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/base/base.json
    ```

3. Update `base_image` variable in `packer/envs/<project>/config.json` with name of created image

4. Build base Mesos image:

    ```
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/base/base_mesos.json
    ```

5. Update `base_mesos` variable in `packer/envs/<project>/config.json` with name of created image

6. Build core base images for JVM and Node.js:

    ```
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/base/base_jvm.json
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/base/base_node.json
    ```

7. Update `base_jvm` and `base_node` variables in `packer/envs/<project>.json` with names of created images

8. Build application-specific base images:

    **Staging**
    ```
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/stage_amigo_server.json
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/stage_backend.json
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/stage_frontend.json
    ```

    **Production**
    ```
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/amigo_server.json
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/db.json
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/es.json
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/es_log.json
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/front_worker.json
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/greenriver.json
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/kafka.json
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/phoenix.json
    $ packer build -only=google -var-file=packer/envs/<project>/config.json packer/vanilla/service_worker.json
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

11. Add new project ID into `projects.json`, with mapping to `foxcommerce-production-shared`.

12. Build inventory:

    ```
    $ make build
    ```

13. Add `bin/envs/<project>` executable:

    ```
    #!/bin/bash

    bin/inventory --env <project> "$@"
    ```

14. Bootstrap initial data for Green River, Kafka, Elasticsearch and PostgreSQL:

    ```
    $ ansible-playbook -v -i bin/envs/<project> ansible/playbook/bootstrap/vanilla.yml
    ```

15. Seed database with demo fixtures:

    ```
    $ ansible-playbook -v -i bin/envs/<project> ansible/playbook/bootstrap/vanilla_seed.yml
    ```

16. Bootstrap Consul Alerts:

    ```
    $ ansible-playbook -v -i bin/envs/<project> ansible/playbook/bootstrap/consul_alerts.yml
    ```

17. Bootstrap Database Backups:

    ```
    $ ansible-playbook -v -i bin/envs/<project> ansible/playbook/bootstrap/db_backup.yml
    ```

## Mesosphere

1. Ensure that Consul, Mesos and Marathon UIs are working. Connect to VPN and open URLs:

    * `http://<amigo_server_0_private_ip>:8500/ui/`
    * `http://<amigo_server_0_private_ip>:5050/#/`
    * `http://<amigo_server_0_private_ip>:8080/ui/#/apps`

2. Check same thing for staging environment, use `stage_amigo` server private IP address.

3. For quicker access to Amigo Servers, configure subdomains in [DNS Simple](https://dnsimple.com). Select [foxcommerce.com](https://dnsimple.com/a/6134/domains/foxcommerce.com) and add related A-records pointing to private IPs. Example: `docker-<project>-stage.foxcommerce.com`

4. Ensure that private Docker Registry is running, open (should return 200 OK):

    * `https://<subdomain>.foxcommerce.com:5000/v2/`

5. Copy [public_key.pem](../../prov-shit/ansible/roles/base/secret_keys/files/public_key.pem) to [ashes](../../ashes) and [firebrand](../../firebrand) subdirectories - they will be copied to related Docker containers. **TBD: Automated key generation for each project.**

6. Build Ashes using Docker and push it to private Docker Registry (both Staging and Production):

    ```
    $ cd highlander/ashes
    $ make build
    $ docker build . --tag ashes
    $ docker tag ashes <subdomain>.foxcommerce.com:5000/ashes
    $ docker push <subdomain>.foxcommerce.com:5000/ashes
    ```

7. Build Storefront using Docker and push it to private Docker Registry (both Staging and Production):

    ```
    $ cd highlander/firebrand
    $ STRIPE_PUBLISHABLE_KEY=<FILL_ME_IN> make build
    $ docker build . --tag storefront
    $ docker tag storefront <subdomain>.foxcommerce.com:5000/storefront
    $ docker push <subdomain>.foxcommerce.com:5000/storefront
    ```

8. Bootstrap Mesos Consul application to auto-register Marathon apps in Consul (both Staging and Production):
    Zookeeper urls will be likely:
     - `<project>-stage-amigo-server:2181` for Staging
     - `<project>-amigo-server-0:2181,<project>-amigo-server-1:2181,<project>-amigo-server-2:2181` for Production

    ```
    $ ansible-playbook -v -i bin/envs/<project> ansible/bootstrap_mesos_consul.yml
    ```

9. Go to Marathon UI and add 2 new applications, using JSON configurations (you should set proper Docker Registry URLs in both files and `STRIPE_PUBLISHABLE_KEY` environment variable for Storefront):

    * [ashes.json](../marathon/ashes.json)
    * [storefront.json](../marathon/storefront.json)

10. Created applications are to be running with a single instance per each app. Perform same actions on staging Marathon, because it uses own Mesos cluster.

11. For Google OAuth, add authorized JavaScript origins and authorized redirect URIs in [Google Developers Console](https://console.developers.google.com/apis/credentials/oauthclient/953682058057-trm1gl4qpa6c9c8av6b42e4p766bloa7.apps.googleusercontent.com?project=foxcomm-staging&authuser=1) using previously created DNS records.
Ensure that same values (client ID, client secret, authorized origins and authorized redirect URIs) are filled on instances that are running `phoenix.service`.

12. Configure `balancer` service to be a reverse proxy for Ashes and Storefront (both Staging and Production):

    ```
    $ ansible-playbook -v i bin/envs/<project> ansible/bootstrap_prod_balancer.yml
    ```

13. Add DNS Simple records pointing to machines with `balancer`. Ensure they have public IPs in Google Cloud and firewall enables HTTPS access. Example:

    * `<project>.foxcommerce.com`
    * `<project>-stage.foxcommerce.com`

14. Perform sanity check for Ashes and Storefront by opening URLs:

    * `https://<project>.foxcommerce.com`
    * `https://<project>.foxcommerce.com/admin`
