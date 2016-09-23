TODO after Target AWS provisioning experience:

* Deal with `/etc/hosts` somehow
	* `phoenix` and `elasticsearch` require this very much
* `hotfix_zk.yml`
* `hotfix_mesos.yml`
* `hotfix_stage_consul.yml`
* Staging Zookeeper: "echo 1 > /var/lib/zookeeper/myid" && restart ZK
* `hotfix_stage_db.yml`
* `/middlewarehouse/sql/flyway.conf` -> replace `localhost` to `db.service.consul`