include ../makelib
header = $(call baseheader, $(1), phoenix-scala)

DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= phoenix
DOCKER_BRANCH ?= master

build:
	$(call header, Building)
	sbt assembly

clean:
	sbt clean

test:
	$(call header, Testing)
	sbt test

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_TAG) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

.PHONY: build test clean docker docker-push
