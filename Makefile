GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

all: build

build:
	$(GO) build -o bin/inventory inventory/inventory.go

lint:
	ansible-lint -x ANSIBLE0007,ANSIBLE0004 ansible/*.yml

test: lint
	$(GO) test -v ./inventory/

bootstrap-prod-small:
	ansible-playbook -v -i bin/envs/prod_small_vpn ansible/bootstrap_prod_small.yml

bootstrap-vanilla-vpn:
	ansible-playbook -v -i bin/envs/vanilla_vpn ansible/bootstrap_vanilla_vpn.yml

bootstrap-vanilla:
	ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_vanilla.yml

bootstrap-vanilla-db:
	ansible-playbook -v -i bin/envs/vanilla ansible/bootstrap_vanilla_db.yml

deploy-stage:
	ansible-playbook -v -i bin/envs/staging ansible/stage.yml

deploy-stage-backend:
	ansible-playbook -v -i bin/envs/staging ansible/stage_backend.yml

deploy-gatling:
	ansible-playbook -v -i bin/envs/staging ansible/gatling.yml

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

deploy-build-agents:
	ansible-playbook -v -i bin/envs/staging ansible/build_agents.yml

.PHONY: all build lint test
