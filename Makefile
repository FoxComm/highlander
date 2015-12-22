GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

all: build

build:
	$(GO) build -o bin/inventory inventory.go

deploy:
	ansible-playbook -i ./staging

test:
	$(GO) test -v ./

.PHONY: all build test
