DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= ashes
DOCKER_BRANCH ?= master

default: test
NODE_ENV = test
CFLAGS = -c -g -D $(NODE_ENV)

# Get version number from package.json, need this for tagging.
version = $(shell iojs -e "console.log(JSON.parse(require('fs').readFileSync('package.json')).version)")

clean:
	rm -rf ./node_modules

test: setup
	@NODE_ENV=$(NODE_ENV) \
		./node_modules/.bin/gulp test

tag:
	git push
	git tag v$(version)
	git push --tags origin master

setup:
	if ! type "yarn" > /dev/null; then npm install -g yarn; fi
	yarn
	yarn build

stop:
	if [ `ps aux | awk '{print $2 " " $11}' | grep [m]-ashes | awk '{print $1}'` ]; then npm stop; fi

run-staging: setup stop
	export NODE_ENV=staging; nohup npm run dev 2>&1 &

run-production: setup stop
	export NODE_ENV=production; nohup ./node_modules/.bin/gulp server 2>&1 &

build-dev: setup
	./node_modules/.bin/flow check
	test -f .env && export eval `cat .env` || true && ON_SERVER=true ./node_modules/.bin/gulp build

build: setup
	./node_modules/.bin/flow check
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ON_SERVER=true ./node_modules/.bin/gulp build

docker:
	docker build -t $(DOCKER_TAG) .

docker-push:
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

.PHONY: build test test-cov tag docker docker-push
