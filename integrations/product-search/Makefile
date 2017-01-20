include ../../makelib
header = $(call baseheader, $(1), product-search)

GOPATH=/tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm
HIGHLANDER_ROOT_PATH=$(BUILD_ROOT_PATH)/highlander
BUILD_PATH=$(HIGHLANDER_ROOT_PATH)/integrations/product-search

DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_IMAGE ?= product-search
DOCKER_TAG ?= master

prep:
	rm $(HIGHLANDER_ROOT_PATH) || true
	mkdir -p $(BUILD_ROOT_PATH)
	ln -s ${CURDIR}/../.. $(HIGHLANDER_ROOT_PATH)
	go get github.com/gin-gonic/gin

clean:
	true

build:
	$(call header, Building)
	make prep
	cd $(BUILD_PATH) && go build -o product-search main.go update_payload.go

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

.PHONY: build test clean docker docker-push
