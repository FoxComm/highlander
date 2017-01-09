include ../makelib
header = $(call baseheader, $(1), prov-shit)

GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

DOCKER_BRANCH ?= master

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

deploy-td-test:
	ansible-playbook -v -i bin/envs/staging ansible/td_test.yml

deploy-tpg-test:
	ansible-playbook -v -i bin/envs/staging ansible/tpg_test.yml

deploy-gatling:
	ansible-playbook -v -i bin/envs/staging ansible/gatling.yml

run-gatling: deploy-gatling
	ansible-playbook -v -i bin/envs/staging ansible/run_gatling.yml

docker:
	$(call header, Dockerizing)
	$(call header, $(DOCKER_BRANCH))
	true

docker-push:
	$(call header, Registering)
	$(call header, $(DOCKER_BRANCH))
	true

.PHONY: all build lint test docker docker-push
