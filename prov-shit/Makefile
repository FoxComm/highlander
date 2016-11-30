include ../makelib
header = $(call baseheader, $(1), prov-shit)

GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

# Main commands
all: build

build:
	$(call header, Building)
	$(GO) build -o bin/inventory inventory/inventory.go

clean:
	true

lint:
	ansible-lint -x ANSIBLE0007,ANSIBLE0004 ansible/*.yml

test:
	$(call header, Testing)
	go get github.com/stretchr/testify
	$(GO) test ./inventory/

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
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

.PHONY: all build lint test docker docker-push
