include ../../../makelib
header = $(call baseheader, $(1), orders-reviews)

export GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/intelligence/consumers/orders-reviews
HIGHLANDER_PATH=$(CURDIR)/../../../

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= orders-reviews
DOCKER_TAG ?= master

prepare:
	$(call header, Prepare)
	rm -rf $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true
	cd $(BUILD_PATH) && glide install
	rm -rf $(BUILD_PATH)/vendor/github.com/FoxComm/highlander

build: prepare
	$(call header, Building)
	cd $(BUILD_PATH) && GOOS=linux CGO_ENABLED=0 go build -o orders-reviews ./*.go

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	docker build --squash --compress -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

clean:
	$(call header, Cleaning)
	rm -rf ./pkg orders-reviews

.PHONY: build test docker docker-push
