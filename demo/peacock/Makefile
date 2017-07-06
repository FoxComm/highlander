include ../../makelib
header = $(call baseheader, $(1), peacock)

DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_IMAGE ?= peacock
DOCKER_TAG ?= master

dev d:
	export eval `cat .env` && yarn dev

setup: clean
	yarn --pure-lockfile

build:
	$(call header, Building)
	make setup
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ./node_modules/.bin/gulp build

docker:
	$(call header, Dockerizing)
	docker build --squash --compress -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

clean: clean-build
	rm -rf ./node_modules

clean-build:
	rm -rf ./build/* ./lib/* ./public/app-*.css ./public/app-*.js

test:
	$(call header, Testing)
	yarn lint
	yarn flow
	yarn test

.PHONY: dev d setup build docker docker-push clean test
