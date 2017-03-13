include ../../makelib
header = $(call baseheader, $(1), river-rock)

export GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/intelligence/river-rock
HIGHLANDER_PATH=$(CURDIR)/../..

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= river-rock
DOCKER_TAG ?= master

prepare:
	$(call header, Prepare)
	rm $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true
	go get github.com/labstack/echo
	go get github.com/orcaman/concurrent-map
	go get github.com/lib/pq

build: prepare
	$(call header, Building)
	go build

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

.PHONY: build test docker docker-push
