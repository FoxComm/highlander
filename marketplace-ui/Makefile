DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= marketplace-ui
DOCKER_BRANCH ?= master

setup:
	npm install

build: setup
	test -f .env && source .env || true && ./node_modules/.bin/gulp build

build-production:
	test -f .env && source .env || true && NODE_ENV=production ./node_modules/.bin/gulp build

package: build
	touch marketplace-ui.tar.bz2
	tar --exclude 'marketplace-ui.tar.bz2' -jcf marketplace-ui.tar.bz2 ./

package-production: build-production
	touch marketplace-ui.tar.bz2
	tar --exclude 'marketplace-ui.tar.bz2' -jcf marketplace-ui.tar.bz2 ./

docker:
	docker build -t $(DOCKER_TAG) .

docker-push:
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

.PHONY: setup build package docker docker-push
