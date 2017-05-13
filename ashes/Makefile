include ../makelib
header = $(call baseheader, $(1), ashes)

export PATH := $(CURDIR)/node_modules/.bin:$(PATH)
SHELL := env PATH=$(PATH) /bin/bash

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

# Linters, utils and cleaners ###

.PHONY: clean
clean:
	rm -rf ./build

.PHONY: test
test: flow lint
	$(call header, Testing)
	rm -rf ./node_modules
	make install
	@NODE_ENV=$(NODE_ENV) gulp test

.PHONY: t
t: flow lint
	@NODE_ENV=$(NODE_ENV) gulp test

.PHONY: lint
lint:
	 eslint --ext .js --ext .jsx ./

.PHONY: flow
flow:
	flow check

.PHONY: build-styleguide
build-styleguide:
	styleguidist build --config styleguide/config.styleguide.js

.PHONY: styleguide
styleguide:
	styleguidist server --config styleguide/config.styleguide.js

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
	 nodemon --watch server --watch config -e js,tmpl ./server/index.js

.PHONY: dev_build
dev_build:
	webpack --progress --colors --watch

.PHONY: dev d
dev d:
	make --jobs dev_build nodemon

# Production ###

.PHONY: prod_build
prod_build: clean
	NODE_ENV=production webpack

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
