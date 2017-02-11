include ../makelib
header = $(call baseheader, $(1), phoenix-scala)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= phoenix
DOCKER_TAG ?= master

build:
	$(call header, Building)
	# sbt assembly

clean:
	sbt clean

test:
	$(call header, Testing)
	# sbt '; scalafmtTestAll ; test'
	make test-seed

test-seed:
	sbt seed

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

.PHONY: build test clean docker docker-push
