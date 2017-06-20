include ../../../makelib
header = $(call baseheader, $(1), product-activity)
export GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/intelligence/consumers/product-activity
HIGHLANDER_PATH=$(CURDIR)/../../../

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= product-activity
DOCKER_TAG ?= master

prepare:
	$(call header, Prepare)
	rm -rf $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true
	go get gopkg.in/olivere/elastic.v3

build: prepare
	$(call header, Building)
	GOOS=linux CGO_ENABLED=0 go build -o product-activity ./*.go

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
	rm -rf ./pkg product-activity

.PHONY: build test docker docker-push
