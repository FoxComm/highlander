include ../makelib
header = $(call baseheader, $(1), search-service)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= search-service
DOCKER_TAG ?= master
SBT_CMD = sbt -DDOCKER_REPO=$(DOCKER_REPO) -DDOCKER_TAG=${DOCKER_TAG}

clean:
	$(call header, Cleaning)
	${SBT_CMD} 'clean'

build: fmt-check
	$(call header, Building)
	${SBT_CMD} '; compile; test:compile'

fmt:
	../utils/scalafmt/scalafmt.sh --diff

fmt-check:
	../utils/scalafmt/scalafmt.sh --test

test:
	$(call header, Testing)
	${SBT_CMD} 'test'

docker:
	$(call header, Dockerizing)
	${SBT_CMD} 'api/docker'

docker-push:
	$(call header, Registering)
	${SBT_CMD} 'api/dockerPush'

docker-build:
	${SBT_CMD} 'api/dockerBuildAndPush'

.PHONY: clean build fmt fmt-check test docker docker-push docker-build
