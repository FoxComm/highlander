include ../../makelib
header = $(call baseheader, $(1), elasticmanager)

GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/tabernacle/elasticmanager
HIGHLANDER_PATH=$(CURDIR)/../..

prepare:
	rm $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true
	cd $(BUILD_PATH) && glide install
	rm -rf $(BUILD_PATH)/vendor/github.com/FoxComm/highlander

build:
	$(call header, Building)
	make prepare
	cd $(BUILD_PATH) && go build -o elasticmanager main.go

docker:
	true

docker-push:
	true
