include ../makelib

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_TAG ?= master
SBT_CMD = sbt -DDOCKER_REPO=$(DOCKER_REPO) -DDOCKER_TAG=${DOCKER_TAG}

clean:
	${SBT_CMD} 'clean'

build:
	${SBT_CMD} '; compile; test:compile'

test:
	${SBT_CMD} 'test'

docker-build:
	${SBT_CMD} 'api/docker'

docker-push:
	${SBT_CMD} 'api/dockerPush'

docker:
	${SBT_CMD} 'api/dockerBuildAndPush'
