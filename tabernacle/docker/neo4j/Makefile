include ../../../makelib
header = $(call baseheader, $(1), neo4j)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= neo4j:3.0
DOCKER_TAG ?= master

build:
	$(call header, Building)

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	docker pull $(DOCKER_IMAGE)

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE)

.PHONY: build test docker docker-push
