include ../makelib
header = $(call baseheader, $(1), cms)

DB=cms_development
DB_TEST=cms_test
DB_USER=cms
GOPATH=/tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/cms
HIGHLANDER_PATH=$(CURDIR)/..

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= cms
DOCKER_TAG ?= master

prepare:
	rm $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true
	cd $(BUILD_PATH) && glide install

build:
	$(call header, Building)
	make prepare
	cd $(BUILD_PATH) && go build -o cms main.go

docker::
	$(call header, Dockerizing)
	cd $(BUILD_PATH) && docker build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push  $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

clean:
	rm -rf ./vendor

test:
	$(call header, Testing)
	true
