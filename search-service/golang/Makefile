include ../../makelib
header = $(call baseheader, $(1), middlewarehouse)

GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/search-service/golang
HIGHLANDER_PATH=$(CURDIR)/../..

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= search-go
DOCKER_TAG ?= master

prepare:
	rm -rf $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true
	cd $(BUILD_PATH) && glide install
	rm -rf $(BUILD_PATH)/vendor/github.com/FoxComm/highlander

####################################################################
# Build targets                                                    #
####################################################################
build:
	$(call header, Building)
	make prepare
	cd $(BUILD_PATH) && go build -o search-service main.go

####################################################################
# Docker build targets                                             #
####################################################################
docker:
	$(call header, Dockerizing)
	cd $(BUILD_PATH) && docker build -t $(DOCKER_IMAGE) .

####################################################################
# Docker push targets                                              #
####################################################################
docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

####################################################################
# Miscellaneous targets                                            #
####################################################################
clean:
	rm -rf ./vendor

test:
	$(call header, Testing)
