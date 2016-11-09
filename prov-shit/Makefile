GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

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

deploy-stage:
	ansible-playbook -v -i bin/envs/staging ansible/stage.yml

deploy-stage-highlander:
	ansible-playbook -v -i bin/envs/staging ansible/highlander_staging.yml

deploy-td-test:
	ansible-playbook -v -i bin/envs/staging ansible/td_test.yml

deploy-tpg-test:
	ansible-playbook -v -i bin/envs/staging ansible/tpg_test.yml

deploy-gatling:
	ansible-playbook -v -i bin/envs/staging ansible/gatling.yml

deploy-gatling-highlander:
	ansible-playbook -v -i bin/envs/staging ansible/highlander_gatling.yml --private-key=$(PRIVATE_KEY)

run-gatling: deploy-gatling
	ansible-playbook -v -i bin/envs/staging ansible/run_gatling.yml

run-gatling-highlander: deploy-gatling-highlander
	ansible-playbook -v -i bin/envs/staging ansible/run_gatling_highlander.yml

docker:
	true

docker-push:
	true

# temp terraform commands
get-cluster-modules:
	terraform get terraform/base/gce_swarm

get-dev-modules:
	terraform get terraform/base/gce_swarm_dev

# production
plan-prod: get-cluster-modules
	terraform plan -state=terraform/envs/gce_swarm_production/terraform.tfstate -var-file=terraform/envs/gce_swarm_production/env.tfvars terraform/base/gce_swarm

plan-destroy-prod: get-cluster-modules
	terraform plan -destroy -state=terraform/envs/gce_swarm_production/terraform.tfstate -var-file=terraform/envs/gce_swarm_production/env.tfvars terraform/base/gce_swarm

apply-prod: get-cluster-modules
	terraform apply -state=terraform/envs/gce_swarm_production/terraform.tfstate -var-file=terraform/envs/gce_swarm_production/env.tfvars terraform/base/gce_swarm

destroy-prod: get-cluster-modules
	terraform destroy -state=terraform/envs/gce_swarm_production/terraform.tfstate -var-file=terraform/envs/gce_swarm_production/env.tfvars terraform/base/gce_swarm

# staging
plan-stage: get-cluster-modules
	terraform plan -state=terraform/envs/gce_swarm_staging/terraform.tfstate -var-file=terraform/envs/gce_swarm_staging/env.tfvars terraform/base/gce_swarm

plan-destroy-stage: get-cluster-modules
	terraform plan -destroy -state=terraform/envs/gce_swarm_staging/terraform.tfstate -var-file=terraform/envs/gce_swarm_staging/env.tfvars terraform/base/gce_swarm

apply-stage: get-cluster-modules
	terraform apply -state=terraform/envs/gce_swarm_staging/terraform.tfstate -var-file=terraform/envs/gce_swarm_staging/env.tfvars terraform/base/gce_swarm

destroy-stage: get-cluster-modules
	terraform destroy -state=terraform/envs/gce_swarm_staging/terraform.tfstate -var-file=terraform/envs/gce_swarm_staging/env.tfvars terraform/base/gce_swarm

# dev
plan-dev: get-dev-modules
	terraform plan -state=terraform/devenvs/alexey/terraform.tfstate -var-file=terraform/devenvs/alexey/env.tfvars terraform/base/gce_swarm_dev

plan-destroy-dev: get-dev-modules
	terraform plan -destroy -state=terraform/devenvs/alexey/terraform.tfstate -var-file=terraform/devenvs/alexey/env.tfvars terraform/base/gce_swarm_dev

apply-dev: get-dev-modules
	terraform apply -state=terraform/devenvs/alexey/terraform.tfstate -var-file=terraform/devenvs/alexey/env.tfvars terraform/base/gce_swarm_dev

destroy-dev: get-dev-modules
	terraform destroy -state=terraform/devenvs/alexey/terraform.tfstate -var-file=terraform/devenvs/alexey/env.tfvars terraform/base/gce_swarm_dev

# all
plan-all: plan-prod plan-stage plan-dev

plan-destroy-all: plan-destroy-prod plan-destroy-stage plan-destroy-dev

apply-all: apply-prod apply-stage apply-dev

destroy-all: destroy-prod destroy-stage destroy-dev

# packer
pack-base:
	packer build -only=google -var-file=packer/envs/swarm/config.json packer/swarm/base.json

pack-master:
	packer build -only=google -var-file=packer/envs/swarm/config.json packer/swarm/master.json

pack-worker:
	packer build -only=google -var-file=packer/envs/swarm/config.json packer/swarm/worker.json

pack-dev:
	packer build -only=google -var-file=packer/envs/swarm/config.json packer/swarm/development.json

# reprovision
provision-prod:
	ansible-playbook -v -i $(m1), ansible/bootstrap_swarm_master.yml \
	--extra-vars @terraform/envs/gce_swarm_production/params.json \
	--extra-vars '{"masters_ips":["$(m1)","$(m2)","$(m3)"]}' \
	--extra-vars datacenter="swarm" \
	--extra-vars consul_leader="$(m1)" \
	--extra-vars mesos_quorum=2 \
	--extra-vars zookeeper_server_id=1 \
	--extra-vars account_file="foxcomm-staging.json" \
	--extra-vars docker_registry_bucket="swarm-production-docker"

	ansible-playbook -v -i $(m2), ansible/bootstrap_swarm_master.yml \
	--extra-vars @terraform/envs/gce_swarm_production/params.json \
	--extra-vars '{"masters_ips":["$(m1)","$(m2)","$(m3)"]}' \
	--extra-vars datacenter="swarm" \
	--extra-vars consul_leader="$(m1)" \
	--extra-vars mesos_quorum=2 \
	--extra-vars zookeeper_server_id=2 \
	--extra-vars account_file="foxcomm-staging.json" \
	--extra-vars docker_registry_bucket="swarm-production-docker"

	ansible-playbook -v -i $(m3), ansible/bootstrap_swarm_master.yml \
	--extra-vars @terraform/envs/gce_swarm_production/params.json \
	--extra-vars '{"masters_ips":["$(m1)","$(m2)","$(m3)"]}' \
	--extra-vars datacenter="swarm" \
	--extra-vars consul_leader="$(m1)" \
	--extra-vars mesos_quorum=2 \
	--extra-vars zookeeper_server_id=3 \
	--extra-vars account_file="foxcomm-staging.json" \
	--extra-vars docker_registry_bucket="swarm-production-docker"

	ansible-playbook -v -i $(w1), ansible/bootstrap_swarm_worker.yml \
	--extra-vars @terraform/envs/gce_swarm_production/params.json \
	--extra-vars '{"masters_ips":["$(m1)","$(m2)","$(m3)"]}' \
	--extra-vars datacenter="swarm" \
	--extra-vars consul_leader="$(m1)"

	ansible-playbook -v -i $(w2), ansible/bootstrap_swarm_worker.yml \
	--extra-vars @terraform/envs/gce_swarm_production/params.json \
	--extra-vars '{"masters_ips":["$(m1)","$(m2)","$(m3)"]}' \
	--extra-vars datacenter="swarm" \
	--extra-vars consul_leader="$(m1)"

provision-stage:
	ansible-playbook -v -i $(m1), ansible/bootstrap_swarm_master.yml \
	--extra-vars @terraform/envs/gce_swarm_staging/params.json \
	--extra-vars '{"masters_ips":["$(m1)"]}' \
	--extra-vars datacenter="swarm" \
	--extra-vars consul_leader="$(m1)" \
	--extra-vars mesos_quorum=1 \
	--extra-vars zookeeper_server_id=1 \
	--extra-vars account_file="foxcomm-staging.json" \
	--extra-vars docker_registry_bucket="swarm-staging-docker"

	ansible-playbook -v -i $(w1), ansible/bootstrap_swarm_worker.yml \
	--extra-vars @terraform/envs/gce_swarm_staging/params.json \
	--extra-vars '{"masters_ips":["$(m1)"]}' \
	--extra-vars datacenter="swarm" \
	--extra-vars consul_leader="$(m1)" \

	ansible-playbook -v -i $(w2), ansible/bootstrap_swarm_worker.yml \
	--extra-vars @terraform/envs/gce_swarm_staging/params.json \
	--extra-vars '{"masters_ips":["$(m1)"]}' \
	--extra-vars datacenter="swarm" \
	--extra-vars consul_leader="$(m1)"

.PHONY: all build lint test
