include ../makelib
header = $(call baseheader, $(1), messaging)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= messaging
DOCKER_TAG ?= master

build:
	$(call header, Building)
	lein uberjar

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

.PHONY: build test docker docker-push
