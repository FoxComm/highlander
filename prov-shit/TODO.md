TODO after Target AWS provisioning experience:

* Export Max's new AWS resources with https://github.com/dtan4/terraforming
* Deal with `/etc/hosts` somehow
	* `phoenix` and `elasticsearch` require this very much
* `hotfix_zk.yml`
* `hotfix_mesos.yml`
* `hotfix_stage_consul.yml`
* Staging Zookeeper: "echo 1 > /var/lib/zookeeper/myid" && restart ZK
* `hotfix_stage_db.yml`
* `/middlewarehouse/sql/flyway.conf` -> replace `localhost` to `db.service.consul`
* Build MWH with GOOS=linux!
* Forgot to use `base_mesos_ami` everywhere, run `make hotfix-mesos-worker`
* TF issue https://github.com/hashicorp/terraform/issues/1652#issuecomment-195164526
