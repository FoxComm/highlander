TODO after Target AWS provisioning experience:

* Export Max's new AWS resources (OpenVPN security groups) with https://github.com/dtan4/terraforming
* Deal with `/etc/hosts` somehow
	* `phoenix`, `elasticsearch` and `schema-registry` require this very much
	* Maybe just turn off this? http://stackoverflow.com/a/33443018
* Hotfix playbooks (to be merged into main playbooks and removed):
	* Fix Zookeeper Elections - [hotfix_zk.yml](https://github.com/FoxComm/highlander/blob/feature/aws-target-cont/prov-shit/ansible/hotfix_zk.yml)
		* Run `PRIVATE_KEY=~/.ssh/tgt.pem make hotfix-zk`
	* Fix Mesos Elections - [hotfix_mesos.yml](https://github.com/FoxComm/highlander/blob/feature/aws-target-cont/prov-shit/ansible/hotfix_mesos.yml)
		* Run `PRIVATE_KEY=~/.ssh/tgt.pem make hotfix-mesos`
	* Fix Mesos Workers - [hotfix_mesos_worker.yml](https://github.com/FoxComm/highlander/blob/feature/aws-target-cont/prov-shit/ansible/hotfix_mesos_worker.yml)
		* Run `PRIVATE_KEY=~/.ssh/tgt.pem make hotfix-mesos-worker`
	* Fix Consul Quorum in Stage - [hotfix_stage_consul.yml](https://github.com/FoxComm/highlander/blob/feature/aws-target-cont/prov-shit/ansible/hotfix_stage_consul.yml)
		* Run `PRIVATE_KEY=~/.ssh/tgt.pem make hotfix-stage-consul`
	* Bootstrap Stage Database - [hotfix_stage_db.yml](https://github.com/FoxComm/highlander/blob/feature/aws-target-cont/prov-shit/ansible/hotfix_stage_db.yml)
		* Run `PRIVATE_KEY=~/.ssh/tgt.pem make hotfix-stage-db`
	* Bootstrap Vanilla Database - [hotfix_db.yml](https://github.com/FoxComm/highlander/blob/feature/aws-target-cont/prov-shit/ansible/hotfix_db.yml)
		* Run `PRIVATE_KEY=~/.ssh/tgt.pem make hotfix-db`
	* Configure Docker Registry for AWS - [hotfix_docker_registry.yml](https://github.com/FoxComm/highlander/blob/feature/aws-target-cont/prov-shit/ansible/hotfix_zk.yml)
		* Run `PRIVATE_KEY=~/.ssh/tgt.pem make hotfix-docker-registry`
* Manual actions (should be fixed in main playbooks):
	* Staging
		* Zookeeper: `echo 1 > /var/lib/zookeeper/myid` && restart ZK
		* `/middlewarehouse/sql/flyway.conf` -> replace `localhost` to `db.service.consul`
		* `/usr/local/bin/run_mesos_master.sh` - QUORUM should be 1
	* Vanilla		
		* Restart kafka & schema-registry after fixing `/etc/hosts`
		* Bottledwaters run script has `db.service` which block it from DNS. set manually to localhost
		* Rollback `/middlewarehouse/sql/flyway.conf` update
* Miscellaneous:
	* Build MWH locally with `GOOS=linux make build`!
	* Forgot to use `base_mesos_ami` everywhere, run `make hotfix-mesos-worker`
	* TF issue https://github.com/hashicorp/terraform/issues/1652#issuecomment-195164526
	* Use `consul members` & `consul force-leave <node_id>` if you dropped previous cluster via terraform