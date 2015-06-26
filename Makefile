default: test
NODE_ENV = test
CFLAGS = -c -g -D $(NODE_ENV)

# Get version number from package.json, need this for tagging.
version = $(shell iojs -e "console.log(JSON.parse(require('fs').readFileSync('package.json')).version)")

test:
	@NODE_ENV=$(NODE_ENV) \
		./node_modules/.bin/gulp mocha

tag:
	git push
	git tag v$(version)
	git push --tags origin master

setup:
	npm install
	npm build

.PHONY: test test-cov tag
