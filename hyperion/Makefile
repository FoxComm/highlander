include ../makelib
header = $(call baseheader, $(1), hyperion)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= hyperion
DOCKER_TAG ?= master

DOCKER_SEEDER_IMAGE ?= hyperion_seeder

build:
	$(call header, Building)
	true

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

docker-seed-push:
	$(call header, Registering)
	docker tag $(DOCKER_SEEDER_IMAGE) $(DOCKER_REPO)/$(DOCKER_SEEDER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_SEEDER_IMAGE):$(DOCKER_TAG)

.PHONY: build test docker docker-push docker-seed-push