include ../makelib
header = $(call baseheader, $(1), marketplace-ui)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= marketplace-ui
DOCKER_TAG ?= master

setup:
	npm install

build:
	$(call header, Building)
	make setup
	test -f .env && source .env || true && ./node_modules/.bin/gulp build

build-production:
	test -f .env && source .env || true && NODE_ENV=production ./node_modules/.bin/gulp build

package: build
	touch marketplace-ui.tar.bz2
	tar --exclude 'marketplace-ui.tar.bz2' -jcf marketplace-ui.tar.bz2 ./

package-production: build-production
	touch marketplace-ui.tar.bz2
	tar --exclude 'marketplace-ui.tar.bz2' -jcf marketplace-ui.tar.bz2 ./

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

.PHONY: setup build package test docker docker-push
