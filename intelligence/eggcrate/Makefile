include ../../makelib
header = $(call baseheader, $(1), eggcrate)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_TAG ?= eggcrate
DOCKER_BRANCH ?= master

build:
	GOOS=linux CGO_ENABLED=0 go build -o eggcrate src/server.go

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_TAG) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

clean:
	rm -rf ./pkg eggcrate