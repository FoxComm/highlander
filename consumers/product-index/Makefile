include ../../makelib
header = $(call baseheader, $(1), product-index)

GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/consumers/product-index
HIGHLANDER_PATH=$(CURDIR)/../..


DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= product-index-consumer
DOCKER_TAG ?= master

prep:
	rm -rf $(GOPATH)
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true

glide: prep
	cd $(BUILD_PATH) && go get github.com/FoxComm/metamorphosis
	cd $(BUILD_PATH) && glide install

build: glide
	cd $(BUILD_PATH) && go build -o product-index-consumer *.go

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

clean:
	rm -rf ./vendor

test:
	cd $(BUILD_PATH) && GOENV=test go test `glide nv`
