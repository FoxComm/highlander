DOCKER_TAG ?= tpg-storefront
DOCKER_BRANCH ?= master

setup:
	npm install

build: setup
	test -f .env && export eval `cat .env` || true && ./node_modules/.bin/gulp build

docker:
	docker build -t $(DOCKER_TAG) .

docker-push:
	docker tag $(DOCKER_TAG) docker-stage.foxcommerce.com:5000/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push docker-stage.foxcommerce.com:5000/$(DOCKER_TAG):$(DOCKER_BRANCH)

clean:
	rm -rf ./node_modules

test:
	npm test

.PHONY: setup build docker docker-push clean test
