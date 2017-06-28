include ../makelib
header = $(call baseheader, $(1), agni)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= agni
DOCKER_TAG ?= master
SBT_CMD = sbt -DDOCKER_REPO=$(DOCKER_REPO) -DDOCKER_TAG=${DOCKER_TAG}
SEARCH_SERVER ?= elasticsearch://10.240.0.18:9300
PORT ?= 9000

autoformat-check:
	../utils/scalafmt/scalafmt.sh --test

run:
	$(call header, Run locally)
	sbt api/run

clean:
	$(call header, Cleaning)
	${SBT_CMD} '; clean'

build: autoformat-check
	$(call header, Building)
	${SBT_CMD} '; compile; test:compile'

test:
	$(call header, Testing)
	${SBT_CMD} '; test'

docker:
	$(call header, Dockerizing)
	${SBT_CMD} '; api/docker'

docker-push:
	$(call header, Registering)
	${SBT_CMD} '; api/dockerPush'

docker-all:
	${SBT_CMD} '; api/dockerBuildAndPush'
