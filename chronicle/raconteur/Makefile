include ../../makelib
header = $(call baseheader, $(1), raconteur)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= raconteur
DOCKER_TAG ?= master

build:
	$(call header, Building)
	GOOS=linux CGO_ENABLED=0 go build -o raconteur ./*.go

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

clean:
	$(call header, Cleaning)
	rm -rf ./pkg raconteur

.PHONY: build test docker docker-push
