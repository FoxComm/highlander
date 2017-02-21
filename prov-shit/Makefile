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

test-go:
	go get github.com/stretchr/testify
	$(GO) test ./inventory/

test-tf:
	./terraform/testfmt.sh

test:
	$(call header, Testing)
	make test-go
	make test-tf

deploy-stage:
	ansible-playbook -v -i bin/envs/staging ansible/test_stage.yml

deploy-demo:
	ansible-playbook -v -i bin/envs/staging ansible/test_stage.yml

docker:
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

.PHONY: all build lint test docker docker-push
