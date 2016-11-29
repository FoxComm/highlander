DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= firebrand
DOCKER_BRANCH ?= master

setup:
	npm install

build:
	@echo "--- Building \033[33mfirebrand\033[0m"
	setup
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ./node_modules/.bin/gulp build

docker:
	@echo "--- Dockerizing \033[33mfirebrand\033[0m"
	docker build -t $(DOCKER_TAG) .

docker-push:
	@echo "--- Registering \033[33mfirebrand\033[0m"
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

clean:
	rm -rf ./node_modules

test:
	@echo "--- Testing \033[33mfirebrand\033[0m"
	setup
	npm test

.PHONY: setup build test docker
