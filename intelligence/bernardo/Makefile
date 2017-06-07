include ../../makelib
header = $(call baseheader, $(1), bernardo)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= bernardo
DOCKER_TAG ?= master

build:
	$(call header, Building)
	mkdir -p build
	cd build && cmake .. && make -j4

test:
	$(call header, Testing)
	true

clean:
	true

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

.PHONY: build clean test docker docker-push
