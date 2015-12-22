GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

all: build

build:
	$(GO) build -o bin/inventory inventory.go

deploy-stage:
	ansible-playbook -i ./staging ansible/stage.yml

test:
	$(GO) test -v ./

.PHONY: all build test
