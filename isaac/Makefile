include ../makelib
header = $(call baseheader, $(1), isaac)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= isaac
DOCKER_TAG ?= master

build:
	$(call header, Building)
	docker build -t $(DOCKER_IMAGE)-build -f Dockerfile.build .
	docker run --name $(DOCKER_IMAGE)-builder $(DOCKER_IMAGE)-build /bin/bash /isaac/build_isaac.sh
	docker cp $(DOCKER_IMAGE)-builder:/isaac/build build
	docker stop $(DOCKER_IMAGE)-builder
	docker rm $(DOCKER_IMAGE)-builder

clean:
	rm -rf ./build

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

.PHONY: build test docker docker-push
