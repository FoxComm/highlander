include ../makelib
header = $(call baseheader, $(1), ashes)

export PATH := $(CURDIR)/node_modules/.bin:$(PATH)
SHELL := env PATH=$(PATH) /bin/sh

GIT_REVISION = $(shell git describe --tags --always)
GIT_COMMIT = $(shell git rev-parse --short HEAD)
WEBPACK_PORT = 4001

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
	rm -rf ./build/admin

.PHONY: test
test: flow lint
	$(call header, Testing)
	rm -rf ./node_modules
	make install
	@NODE_ENV=$(NODE_ENV) gulp test

.PHONY: t
t: flow lint
	@NODE_ENV=$(NODE_ENV) gulp test

.PHONY: mocha-main
mocha-main:
	@NODE_ENV=$(NODE_ENV) gulp mocha.main

.PHONY: mocha-unit
mocha-unit:
	@NODE_ENV=$(NODE_ENV) gulp mocha.unit

.PHONY: lint
lint:
	 eslint --ext .js --ext .jsx ./

.PHONY: flow
flow:
	flow check

.PHONY: build-styleguide
build-styleguide:
	GIT_COMMIT=$(GIT_COMMIT) styleguidist build --config styleguide/config.styleguide.js

.PHONY: styleguide sg
styleguide sg:
	GIT_COMMIT=$(GIT_COMMIT) styleguidist server --config styleguide/config.styleguide.js

# Base ###

.PHONY: install i
install i:
	yarn cache clean
	yarn --pure-lockfile

# Development ###

.PHONY: nodemon
nodemon:
	WEBPACK_PORT=$(WEBPACK_PORT)  nodemon --watch server --watch .env --watch config -e js,tmpl,env \
	--exec "node -r dotenv/config" ./server/index.js

.PHONY: build-dev
build-dev:
	GIT_REVISION=$(GIT_REVISION) webpack-dev-server \
	--inline --watch --hot --progress --content-base=build/ --port $(WEBPACK_PORT)

.PHONY: dev d
dev d:
	make --jobs build-dev nodemon

# Production ###

.PHONY: build-prod
build-prod: clean
	GIT_REVISION=$(GIT_REVISION) NODE_ENV=production webpack --progress

.PHONY: prod p
prod p: build-prod
	NODE_ENV=production node -r dotenv/config ./server/index.js

# Buildkite ###

.PHONY: build
build:
	$(call header, Building)
	make install
	BEHIND_NGINX=true make build-prod
ifeq ($(ASHES_BUILD_STYLEGUIDE), true)
	make build-styleguide
endif
