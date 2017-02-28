include ../../makelib
header = $(call baseheader, $(1), user-simulation)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_TAG ?= user-simulation
DOCKER_BRANCH ?= master

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

.PHONY: build test docker docker-push
