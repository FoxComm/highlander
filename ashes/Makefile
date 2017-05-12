include ../makelib
header = $(call baseheader, $(1), ashes)

# Docker stuff ###

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= ashes
DOCKER_TAG ?= master

default: test
NODE_ENV = test
CFLAGS = -c -g -D $(NODE_ENV)

# Get version number from package.json, need this for tagging.
version = $(shell iojs -e "console.log(JSON.parse(require('fs').readFileSync('package.json')).version)")

.PHONY: docker
docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_IMAGE) .

.PHONY: docker-push
docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

.PHONY: tag
tag:
	git push
	git tag v$(version)
	git push --tags origin master

# Linters and cleaners ###

.PHONY: clean
clean:
	rm -rf ./build

.PHONY: test
test: flow
	$(call header, Testing)
	make install
	@NODE_ENV=$(NODE_ENV) ./node_modules/.bin/gulp test

.PHONY: t
t:
	make flow
	@NODE_ENV=$(NODE_ENV) ./node_modules/.bin/gulp test

.PHONY: lint
lint:
	@NODE_ENV=$(NODE_ENV) ./node_modules/.bin/gulp lint

.PHONY: flow
flow:
	./node_modules/.bin/flow check

# Base ###

.PHONY: install
install:
	yarn cache clean
	yarn --pure-lockfile

.PHONY: rev
rev:
	git describe --always | cat > .git-rev

# Development ###

.PHONY: nodemon
nodemon: rev
	test -f .env && export eval `cat .env` || true &&\
	 ./node_modules/.bin/nodemon --watch server --watch config -e js,tmpl ./server/index.js

.PHONY: dev_build
dev_build:
	./node_modules/.bin/webpack --progress --colors --watch

.PHONY: dev d
dev d:
	make --jobs dev_build nodemon

.PHONY: build-styleguide
build-styleguide:
	./node_modules/.bin/styleguidist build --config styleguide/config.styleguide.js

# Production ###

.PHONY: prod_build
prod_build: clean
	NODE_ENV=production ./node_modules/.bin/webpack

.PHONY: prod p
prod p: prod_build
	test -f .env && export eval `cat .env` || true && NODE_ENV=production node ./server/index.js

# Buildkite ###

.PHONY: build
build:
	$(call header, Building)
	make rev
	make install
	BEHIND_NGINX=true make prod_build
ifeq ($(ASHES_BUILD_STYLEGUIDE), true)
	make build-styleguide
endif
