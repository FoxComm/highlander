include ../makelib
header = $(call baseheader, $(1), remote)

GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/remote
HIGHLANDER_PATH=$(CURDIR)/..

CMD_PREFIX=cd $(BUILD_PATH) && GOPATH=$(GOPATH)
GO=$(CMD_PREFIX) go
GO_TEST=$(CMD_PREFIX) GOENV=test go test
GLIDE=$(CMD_PREFIX) glide
DOCKER=$(CMD_PREFIX) docker

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= remote
DOCKER_TAG ?= master

prepare:
	rm -rf $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true
	$(GLIDE) install
	rm -rf $(BUILD_PATH)/vendor/github.com/FoxComm/highlander

build:
	$(call header, Building)
	make prepare
	$(GO) build -o remote main.go

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	$(DOCKER) build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	$(DOCKER) tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	$(DOCKER) push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
