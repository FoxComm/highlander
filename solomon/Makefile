DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= solomon
DOCKER_BRANCH ?= master

build:
	true

test:
	true

docker:
	docker build -t $(DOCKER_TAG) .

docker-push:
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

.PHONY: build test
