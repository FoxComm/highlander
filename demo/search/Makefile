DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_IMAGE ?= demo-search
DOCKER_TAG ?= master

setup:
	true

build:
	npm install

docker:
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

.PHONY: setup build docker docker-push
