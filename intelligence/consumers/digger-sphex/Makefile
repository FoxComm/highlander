include ../../../makelib
header = $(call baseheader, $(1), digger-sphex)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= digger-sphex
DOCKER_TAG ?= master

build:
	$(call header, Building)
	true

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

clean:
	$(call header, Cleaning)
	true

.PHONY: build test docker docker-push
