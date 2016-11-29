DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= marketplace-ui
DOCKER_BRANCH ?= master

setup:
	npm install

build:
	@echo "--- Building \033[33mmarketplace-ui\033[0m"
	setup
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
	@echo "--- Testing \033[33mmarketplace-ui\033[0m"
	true

docker:
	@echo "--- Dockerizing \033[33mmarketplace-ui\033[0m"
	docker build -t $(DOCKER_TAG) .

docker-push:
	@echo "--- Registering \033[33mmarketplace-ui\033[0m"
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

.PHONY: setup build package test docker docker-push
