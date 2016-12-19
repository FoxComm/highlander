DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= tpg-storefront
DOCKER_BRANCH ?= master

dev d:
	source .env && npm run dev

setup:
	rm -rf ./node_modules
	npm install

build: setup
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ./node_modules/.bin/gulp build

docker:
	docker build -t $(DOCKER_TAG) .

docker-push:
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

clean:
	rm -rf ./node_modules

test:
	npm test

.PHONY: dev d setup build docker docker-push clean test
