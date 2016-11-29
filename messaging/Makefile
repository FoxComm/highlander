include ../makelib
header = $(call baseheader, $(1), messaging)

DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= messaging
DOCKER_BRANCH ?= master

build:
	$(call header, Building)
	lein uberjar

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_TAG) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

test:
	$(call header, Testing)
	true

.PHONY: build test docker docker-push
