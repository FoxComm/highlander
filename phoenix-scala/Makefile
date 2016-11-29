DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= phoenix
DOCKER_BRANCH ?= master

build:
	@echo "--- Building \033[33mphoenix-scala\033[0m"
	sbt assembly

clean:
	sbt clean

test:
	@echo "--- Testing \033[33mphoenix-scala\033[0m"
	sbt test

docker:
	@echo "--- Dockerizing \033[33mphoenix-scala\033[0m"
	docker build -t $(DOCKER_TAG) .

docker-push:
	@echo "--- Registering \033[33mphoenix-scala\033[0m"
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

.PHONY: build test clean docker docker-push
