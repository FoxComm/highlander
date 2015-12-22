GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

all: build

build:
	$(GO) build -o bin/inventory inventory.go

deploy:
	ansible-playbook -i ./staging

lint:
	ansible-lint ansible/*.yml

test: lint
	$(GO) test -v ./

.PHONY: all build lint test
