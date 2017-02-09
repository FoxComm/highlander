include ../../../makelib
header = $(call baseheader, $(1), digger-sphex)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_TAG ?= digger-sphex
DOCKER_BRANCH ?= master

build:
	$(call header, Building)
	true

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_TAG) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

clean:
	$(call header, Cleaning)
	true

.PHONY: build test docker docker-push
