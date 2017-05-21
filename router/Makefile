include ../makelib
header = $(call baseheader, $(1), router)

GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/router
HIGHLANDER_PATH=$(CURDIR)/..

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= router
DOCKER_TAG ?= master

prepare:
	rm -rf $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true

build:
	$(call header, Building)
	make prepare
	cd $(BUILD_PATH) && go build -o router main.go proxy.go
