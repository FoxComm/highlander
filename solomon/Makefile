DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= solomon
DOCKER_BRANCH ?= master

build:
	@echo "--- Building \033[33msolomon\033[0m"
	true

test:
	@echo "--- Testing \033[33msolomon\033[0m"
	true

docker:
	@echo "--- Dockerizing \033[33msolomon\033[0m"
	docker build -t $(DOCKER_TAG) .

docker-push:
	@echo "--- Registering \033[33msolomon\033[0m"
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

.PHONY: build test
