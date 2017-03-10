include ../makelib
header = $(call baseheader, $(1), ashes)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= ashes
DOCKER_TAG ?= master

NODE_MODULES ?= node_modules
export PATH := $(PWD)/$(NODE_MODULES)/.bin:$(PATH)
export NODE_MODULES := $(NODE_MODULES)
SHELL := env PATH=$(PATH) /bin/bash

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
	make t

t:
	@NODE_ENV=$(NODE_ENV) gulp test

tag:
	git push
	git tag v$(version)
	git push --tags origin master

setup:
	yarn --pure-lockfile
	yarn build

stop:
	if [ `ps aux | awk '{print $2 " " $11}' | grep [m]-ashes | awk '{print $1}'` ]; then npm stop; fi

run-staging: setup stop
	export NODE_ENV=staging; nohup npm run dev 2>&1 &

run-production: setup stop
	export NODE_ENV=production; nohup gulp server 2>&1 &

# @todo these hooks should be only for ashes, not for highlander
# Auto-install and update git hooks
# HOOKS_SRC = tools/hooks
# HOOKS_DST = ../.git/hooks
# GIT_HOOKS = $(addprefix $(HOOKS_DST)/, $(notdir $(wildcard $(HOOKS_SRC)/*)))

# $(HOOKS_DST)/%: $(HOOKS_SRC)/%
# 	@cp -p $< $@
# 	@chmod a+x $@

# hooks: $(GIT_HOOKS)

rev:
	git describe --always | cat > .git-rev

dev d: rev
	test -f .env && export eval `cat .env` || true && gulp dev

prod p:
	test -f .env && export eval `cat .env` || true && NODE_ENV=production gulp build
	test -f .env && export eval `cat .env` || true && NODE_ENV=production node boot.js

flow:
	flow check

build-dev: setup flow
	test -f .env && export eval `cat .env` || true && ON_SERVER=true gulp build

build: rev flow
	$(call header, Building)
	make setup
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ON_SERVER=true gulp build

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_IMAGE) .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

.PHONY: hooks rev dev d prod p build test test-cov tag docker docker-push
