default: test
NODE_ENV = test
REPORTER = dot
MOCHA_OPTS =
CFLAGS = -c -g -D $(NODE_ENV)

# Find all test files
SPECS = $(shell find ./test/specs -type f -name "*.js")
ACCEPTANCE = $(shell find ./test/acceptance -type f -name "*.js")

# Get version number from package.json, need this for tagging.
version = $(shell iojs -e "console.log(JSON.parse(require('fs').readFileSync('package.json')).version)")

test:
	@NODE_ENV=$(NODE_ENV) \
		./node_modules/.bin/mocha \
		--harmony \
		--reporter $(REPORTER) \
		$(MOCHA_OPTS) \
		--require co-mocha \
		--timeout 10s \
		--bail \
		test/spec-helper.js \
		$(SPECS) \
		$(ACCEPTANCE)

test-cov:
	@NODE_COV=1 $(MAKE) test MOCHA_OPTS='--require blanket' REPORTER=html-cov > coverage.html

tag:
	git push
	git tag v$(version)
	git push --tags origin master

setup:
	npm install
	npm build

.PHONY: test test-cov tag
