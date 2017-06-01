include ../makelib

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_TAG ?= master
SBT_CMD = sbt -DDOCKER_REPO=$(DOCKER_REPO) -DDOCKER_TAG=${DOCKER_TAG}

clean:
	${SBT_CMD} 'clean'


build-akka:
	${SBT_CMD} 'akka/assembly'

build-finch:
	${SBT_CMD} 'finch/assembly'

build-http4s:
	${SBT_CMD} 'http4s/assembly'

build:
	${SBT_CMD} '; akka/assembly; finch/assembly; http4s/assembly'


docker-build-akka:
	${SBT_CMD} 'akka/docker'

docker-build-finch:
	${SBT_CMD} 'finch/docker'

docker-build-http4s:
	${SBT_CMD} 'http4s/docker'

docker-build:
	${SBT_CMD} '; akka/docker; finch/docker; http4s/docker'


docker-push-akka:
	${SBT_CMD} 'akka/dockerPush'

docker-push-finch:
	${SBT_CMD} 'finch/dockerPush'

docker-push-http4s:
	${SBT_CMD} 'http4s/dockerPush'

docker-push:
	${SBT_CMD} '; akka/dockerPush; finch/dockerPush; http4s/dockerPush'


docker-akka:
	${SBT_CMD} 'akka/dockerBuildAndPush'

docker-finch:
	${SBT_CMD} 'finch/dockerBuildAndPush'

docker-http4s:
	${SBT_CMD} 'http4s/dockerBuildAndPush'

docker:
	${SBT_CMD} '; akka/dockerBuildAndPush; finch/dockerBuildAndPush; http4s/dockerBuildAndPush'
