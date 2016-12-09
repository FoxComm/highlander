DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= demo-search
DOCKER_BRANCH ?= master

setup:
	true

build: 
	npm install

docker:
	docker build -t $(DOCKER_TAG) .

docker-push:
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

.PHONY: setup build docker docker-push
