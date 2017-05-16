DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_IMAGE ?= nexus
DOCKER_TAG ?= master

dev d:
	export eval `cat .env` && yarn dev

setup: clean
	yarn --pure-lockfile

build: setup
	test -f .env && export eval `cat .env` || true && NODE_ENV=production yarn build

check:
	yarn lint
	yarn flow

docker:
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

clean: clean-build
	rm -rf ./node_modules

clean-build:
	rm -rf ./public/app-*.css ./public/app-*.js

test: check
	yarn test

fmt:
	yarn fmt

.PHONY: dev d setup build docker docker-push clean clean-build test check fmt
