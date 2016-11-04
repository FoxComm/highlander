DOCKER_HOST ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= messaging
DOCKER_BRANCH ?= master

build:
	lein uberjar

docker: build
	docker build -t $(DOCKER_TAG) .

docker-push: docker
	docker tag $(DOCKER_TAG) $(DOCKER_HOST)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_HOST)/$(DOCKER_TAG):$(DOCKER_BRANCH)

test:
	true

.PHONY: build test
