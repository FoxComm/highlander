include ../makelib
header = $(call baseheader, $(1), ashes)

export PATH := $(CURDIR)/node_modules/.bin:$(PATH)
SHELL := env PATH=$(PATH) /bin/sh

export GIT_REVISION = $(shell git describe --tags --always)
export GIT_COMMIT = $(shell git rev-parse --short HEAD)
export URL_PREFIX = /admin
WEBPACK_PORT = 4001

notify:
	$(call header, $(MSG))

# Docker stuff ###

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= ashes
DOCKER_TAG ?= master

default: test
NODE_ENV = test
CFLAGS = -c -g -D $(NODE_ENV)

# Get version number from package.json, need this for tagging.
version = $(shell iojs -e "console.log(JSON.parse(require('fs').readFileSync('package.json')).version)")

PRETTIER_OPTIONS = --single-quote --trailing-comma es5 --print-width 120

# list of js(x) files in git diff that are not formatted in Prettier style
JS_FILES_DIFF = $(shell git diff HEAD --name-only --diff-filter=ACM | grep '\.jsx\?$$' | sed 's/ashes\///g' | tr '\n' ' ')
JS_FILES_TO_PRETTIFY = $(shell [[ !  -z  "$(JS_FILES_DIFF)"  ]] && \
	prettier $(PRETTIER_OPTIONS) -l $(JS_FILES_DIFF))

.PHONY: docker
docker: MSG=Dockerizing
docker: notify
	docker build -t $(DOCKER_IMAGE) .

.PHONY: docker-push
docker-push: MSG=Registering
docker-push: notify
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
test: MSG=Testing
test: notify flow lint
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

# run prettier against js(x) files in git diff that are not formatted in Prettier style
.PHONY: fmt
fmt:
ifneq ($(JS_FILES_TO_PRETTIFY),)
	prettier $(PRETTIER_OPTIONS) --write $(JS_FILES_TO_PRETTIFY)
endif

# prettierify target directory. usage: make fmt-config
fmt-%:
	prettier $(PRETTIER_OPTIONS) --write "$*/**/*.{js,jsx}"

.PHONY: build-styleguide
build-styleguide:
	styleguidist build --config styleguide/config.styleguide.js

.PHONY: styleguide sg
styleguide sg:
	styleguidist server --config styleguide/config.styleguide.js

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
	webpack-dev-server \
	--inline --watch --hot --progress --content-base=build/ --port $(WEBPACK_PORT)

.PHONY: dev d
dev d:
	make --jobs build-dev nodemon

# Production ###

.PHONY: build-prod
build-prod:
	NODE_ENV=production webpack --progress

.PHONY: prod p
prod p: clean build-prod
	NODE_ENV=production node -r dotenv/config ./server/index.js

# Buildkite ###

.PHONY: build
build: MSG=Building
build: notify clean install build-styleguide build-prod
