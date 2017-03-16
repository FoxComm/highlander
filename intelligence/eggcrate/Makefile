include ../../makelib
header = $(call baseheader, $(1), eggcrate)

export GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/intelligence/eggcrate
HIGHLANDER_PATH=$(CURDIR)/../..

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= eggcrate
DOCKER_TAG ?= master

prepare:
	$(call header, Prepare)
	rm $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true
	go get github.com/labstack/echo

build: prepare
	GOOS=linux CGO_ENABLED=0 go build -o eggcrate src/server.go

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

test:
	$(call header, Testing)
	true

clean:
	rm -rf ./pkg eggcrate
