GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

all: build

build:
	$(GO) build -o bin/inventory inventory.go

seed-cloud:
	terraform apply terraform/

deploy-stage:
	ansible-playbook -i ./staging ansible/stage.yml

deploy-gatling: seed-cloud
	ansible-playbook -i ./staging ansible/gatling.yml

lint:
	ansible-lint ansible/*.yml

test: lint
	$(GO) test -v ./

.PHONY: all build lint test
