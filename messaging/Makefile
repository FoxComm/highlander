DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= messaging
DOCKER_BRANCH ?= master

build:
	lein uberjar

docker:
	docker build -t $(DOCKER_TAG) .

docker-push:
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

test:
	true

.PHONY: build test
