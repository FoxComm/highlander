include ../makelib
header = $(call baseheader, $(1), ashes)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= ashes
DOCKER_TAG ?= master

default: test
NODE_ENV = test
CFLAGS = -c -g -D $(NODE_ENV)

# Get version number from package.json, need this for tagging.
version = $(shell iojs -e "console.log(JSON.parse(require('fs').readFileSync('package.json')).version)")

clean:
	rm -rf ./node_modules

test:
	$(call header, Testing)
	make setup
	@NODE_ENV=$(NODE_ENV) ./node_modules/.bin/gulp test

t:
	make flow
	@NODE_ENV=$(NODE_ENV) ./node_modules/.bin/gulp test

lint:
	@NODE_ENV=$(NODE_ENV) ./node_modules/.bin/gulp lint

tag:
	git push
	git tag v$(version)
	git push --tags origin master

setup:
	yarn cache clean
	yarn --pure-lockfile
	yarn build

stop:
	if [ `ps aux | awk '{print $2 " " $11}' | grep [m]-ashes | awk '{print $1}'` ]; then npm stop; fi

run-staging: setup stop
	export NODE_ENV=staging; nohup npm run dev 2>&1 &

run-production: setup stop
	export NODE_ENV=production; nohup ./node_modules/.bin/gulp server 2>&1 &

rev:
	git describe --always | cat > .git-rev

dev d: rev
	test -f .env && export eval `cat .env` || true && ./node_modules/.bin/gulp dev

prod p:
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ./node_modules/.bin/gulp build
	test -f .env && export eval `cat .env` || true && NODE_ENV=production node boot.js

.PHONY: flow
flow:
	./node_modules/.bin/flow check

build-styleguide:
	yarn run styleguide:build

build-dev: setup flow
	test -f .env && export eval `cat .env` || true && BEHIND_NGINX=true ./node_modules/.bin/gulp build

build: 
	$(call header, Building)
	make rev 
	make setup
	make flow
	test -f .env && export eval `cat .env` || true && NODE_ENV=production BEHIND_NGINX=true ./node_modules/.bin/gulp build

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

.PHONY: hooks rev dev d prod p build test test-cov tag docker docker-push
