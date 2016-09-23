TODO after Target AWS provisioning experience:

* Export Max's new AWS resources with https://github.com/dtan4/terraforming
* Deal with `/etc/hosts` somehow
	* `phoenix`, `elasticsearch` and `schema-registry` require this very much
	* Maybe just turn off this? http://stackoverflow.com/a/33443018
* Hotfix playbooks:
	* `hotfix_zk.yml`
	* `hotfix_mesos.yml`
	* `hotfix_stage_consul.yml`
	* `hotfix_stage_db.yml`
	* `hotfix_db.yml`
* Manual actions:
	* Staging Zookeeper: "echo 1 > /var/lib/zookeeper/myid" && restart ZK
	* `/middlewarehouse/sql/flyway.conf` -> replace `localhost` to `db.service.consul` for staging
		* rollback for vanilla
	* Build MWH with GOOS=linux!
	* Forgot to use `base_mesos_ami` everywhere, run `make hotfix-mesos-worker`
	* TF issue https://github.com/hashicorp/terraform/issues/1652#issuecomment-195164526
	* Use `consul members` & `consul force-leave <node_id>` if you dropped previous cluster via terraform
	* vanilla: restart kafka & schema-registry after fixing `/etc/hosts`
	* vanilla: bottledwaters run script has `db.service` which block it from DNS. set manually to localhost