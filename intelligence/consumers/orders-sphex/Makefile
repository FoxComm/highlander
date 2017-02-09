include ../../../makelib
header = $(call baseheader, $(1), orders-sphex)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= orders-sphex
DOCKER_TAG ?= master

build:
	GOOS=linux CGO_ENABLED=0 go build -o orders-sphex ./*.go

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

clean:
	rm -rf ./pkg orders-sphex
