include ../makelib
header = $(call baseheader, $(1), green-river)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= greenriver
DOCKER_TAG ?= master

build: fmt-check
	$(call header, Building)
	sbt assembly

fmt:
	../utils/scalafmt/scalafmt.sh --diff

fmt-check:
	../utils/scalafmt/scalafmt.sh --test

clean:
	sbt clean

test:
	$(call header, Testing)
	../utils/scalafmt/scalafmt.sh --test &&	sbt test

docker:
	$(call header, Dockerizing)
	docker build --compress -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

.PHONY: build test clean docker docker-push fmt fmt-check
