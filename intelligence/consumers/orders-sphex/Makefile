include ../../../makelib
header = $(call baseheader, $(1), orders-sphex)
export GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/intelligence/consumers/orders-sphex
HIGHLANDER_PATH=$(CURDIR)/../../../

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= orders-sphex
DOCKER_TAG ?= master

prepare:
	$(call header, Prepare)
	rm $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true
	go get github.com/FoxComm/metamorphosis

build: prepare
	$(call header, Building)
	GOOS=linux CGO_ENABLED=0 go build -o orders-sphex ./*.go

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

clean:
	$(call header, Cleaning)
	rm -rf ./pkg orders-sphex

.PHONY: build test docker docker-push
