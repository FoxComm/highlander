GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

all: build

build:
	$(GO) build -o bin/inventory inventory.go

deploy-stage:
	ansible-playbook -v -i ./staging ansible/stage.yml

deploy-gatling:
	ansible-playbook -v  -i ./staging ansible/gatling.yml

run-gatling: deploy-gatling
	ansible-playbook -v -i ./staging ansible/run_gatling.yml

deploy-demo:
	ansible-playbook -v -i ./staging ansible/demo.yml

deploy-demo2:
	ansible-playbook -v -i ./staging ansible/demo2.yml

deploy-usertest1:
	ansible-playbook -v -i ./staging ansible/usertest1.yml

deploy-usertest2:
	ansible-playbook -v -i ./staging ansible/usertest2.yml

deploy-dem1:
	ansible-playbook -v -i ./staging ansible/dem1.yml

deploy-dem2:
	ansible-playbook -v -i ./staging ansible/dem2.yml

deploy-build-agents:
	ansible-playbook -v -i ./staging ansible/build_agents.yml

deploy-prod-small:
	ansible-playbook -v -i ./staging ansible/prod_small.yml

lint:
	ansible-lint ansible/*.yml

test: lint
	$(GO) test -v ./

.PHONY: all build lint test
