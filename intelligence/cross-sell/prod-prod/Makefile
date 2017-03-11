include ../../../makelib
header = $(call baseheader, $(1), prod-prod)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= prod-prod
DOCKER_TAG ?= master

build:
	$(call header, Building)
	true

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

clean:
	$(call header, Cleaning)
	true

.PHONY: build test docker docker-push
