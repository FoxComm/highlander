include ../makelib
header = $(call baseheader, $(1), firebrand)

DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= firebrand
DOCKER_BRANCH ?= master

setup:
	npm install

build:
	$(call header, Building)
	make setup
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ./node_modules/.bin/gulp build

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_TAG) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

clean:
	rm -rf ./node_modules

test:
	$(call header, Testing)
	make setup
	npm test

.PHONY: setup build test docker
