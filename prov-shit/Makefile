GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

TF_CMD ?= plan
TF_BASE = terraform/base
TF_ENVS = terraform/envs
PRIVATE_KEY ?= ~/.ssh/id_rsa

# Main commands
all: build

build:
	$(GO) build -o bin/inventory inventory/inventory.go

clean:
	true

lint:
	ansible-lint -x ANSIBLE0007,ANSIBLE0004 ansible/*.yml

test:
	go get github.com/stretchr/testify
	$(GO) test -v ./inventory/

bootstrap-build-agents:
	ansible-playbook -v -i bin/envs/staging ansible/bootstrap_build_agents.yml

bootstrap-consul-alerts:
	ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_consul_alerts.yml

bootstrap-db-backup:
	ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_db_backup.yml

bootstrap-messaging-plugin:
	ansible-playbook -v -i bin/envs/staging ansible/boostrap_messaging_plugin.yml

bootstrap-middlewarehouse:
	ansible-playbook -v -i bin/envs/staging ansible/bootstrap_middlewarehouse.yml --private-key=$(PRIVATE_KEY)

bootstrap-tinystack:
	ansible-playbook -v -i bin/envs/staging ansible/bootstrap_tinystack.yml --private-key=$(PRIVATE_KEY)

bootstrap-vanilla:
	ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_vanilla.yml

bootstrap-vanilla-db:
	ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_vanilla_db.yml

bootstrap-vanilla-openvpn-key:
	ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_openvpn_key.yml

bootstrap-vanilla-vpn:
	ansible-playbook -v -i bin/envs/vanilla_vpn ansible/bootstrap_vanilla_vpn.yml

bootstrap-target:
	ansible-playbook -v -i bin/envs/target ansible/bootstrap_vanilla.yml

bootstrap-target-openvpn-key:
	ansible-playbook -v -i bin/envs/target ansible/bootstrap_openvpn_key.yml

bootstrap-target-vpn:
	ansible-playbook -v -i bin/envs/target_vpn ansible/bootstrap_vanilla_vpn.yml

deploy-stage:
	ansible-playbook -v -i bin/envs/staging ansible/stage.yml

deploy-stage-highlander:
	ansible-playbook -v -i bin/envs/staging ansible/stage_highlander.yml

deploy-gatling:
	ansible-playbook -v -i bin/envs/staging ansible/gatling.yml

deploy-gatling-highlander:
	ansible-playbook -v -i bin/envs/staging ansible/gatling_highlander.yml

run-gatling: deploy-gatling
	ansible-playbook -v -i bin/envs/staging ansible/run_gatling.yml

deploy-usertest1:
	ansible-playbook -v -i bin/envs/staging ansible/usertest1.yml

deploy-usertest2:
	ansible-playbook -v -i bin/envs/staging ansible/usertest2.yml

deploy-dem1:
	ansible-playbook -v -i bin/envs/staging ansible/dem1.yml

deploy-dem2:
	ansible-playbook -v -i bin/envs/staging ansible/dem2.yml

tf-aws-target:
	terraform $(TF_CMD) -state=$(TF_ENVS)/aws_target/terraform.tfstate -var-file=$(TF_ENVS)/aws_target/target.tfvars $(TF_BASE)/aws_target

tf-stage:
	terraform $(TF_CMD) -state=$(TF_ENVS)/gce_dev/terraform.tfstate -var-file=$(TF_ENVS)/gce_dev/dev.tfvars $(TF_BASE)/gce_dev

tf-vanilla:
	terraform $(TF_CMD) -state=$(TF_ENVS)/gce_vanilla/terraform.tfstate -var-file=$(TF_ENVS)/gce_vanilla/vanilla.tfvars $(TF_BASE)/gce_vanilla

tf-target:
	terraform $(TF_CMD) -state=$(TF_ENVS)/gce_target/terraform.tfstate -var-file=$(TF_ENVS)/gce_target/target.tfvars $(TF_BASE)/gce_target

tf-topdrawer:
	terraform $(TF_CMD) -state=$(TF_ENVS)/gce_topdrawer/terraform.tfstate -var-file=$(TF_ENVS)/gce_topdrawer/topdrawer.tfvars $(TF_BASE)/gce_topdrawer

.PHONY: all build lint test
