GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

all: build

build:
	$(GO) build -o bin/inventory inventory.go

seed-cloud:
	terraform apply terraform/

deploy-stage:
	ansible-playbook -v -i ./staging ansible/stage.yml

deploy-gatling: seed-cloud
	ansible-playbook -v  -i ./staging ansible/gatling.yml

run-gatling: deploy-gatling
	ansible-playbook -v -i ./staging ansible/run_gatling.yml

deploy-demo: seed-cloud
	ansible-playbook -v -i ./staging ansible/demo.yml

lint:
	ansible-lint ansible/*.yml

test: lint
	$(GO) test -v ./

.PHONY: all build lint test
