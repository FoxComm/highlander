BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/integrations/shipstation
HIGHLANDER_PATH=$(CURDIR)/../..

DOCKER_HOST ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= shipstation-consumer
DOCKER_BRANCH ?= master

glide:
	cd $(BUILD_PATH) && glide install

build: glide
	cd $(BUILD_PATH) && go build -o shipstation main.go

build-linux: glide
	cd $(BUILD_PATH) && GOOS=linux $(MAKE) build

docker:
	docker build -t $(DOCKER_TAG) .

docker-push:
	docker tag $(DOCKER_TAG) $(DOCKER_HOST)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_HOST)/$(DOCKER_TAG):$(DOCKER_BRANCH)

.PHONY: build
