DOCKER_HOST ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= firebrand
DOCKER_BRANCH ?= master

setup:
	npm install

build: setup
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ./node_modules/.bin/gulp build

docker:
	docker build -t $(DOCKER_TAG) .

docker-push:
	docker tag $(DOCKER_TAG) $(DOCKER_HOST)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_HOST)/$(DOCKER_TAG):$(DOCKER_BRANCH)

clean:
	rm -rf ./node_modules

test: setup
	npm test

.PHONY: setup build test docker
