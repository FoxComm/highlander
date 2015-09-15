default: test
NODE_ENV = test
CFLAGS = -c -g -D $(NODE_ENV)

# Get version number from package.json, need this for tagging.
version = $(shell iojs -e "console.log(JSON.parse(require('fs').readFileSync('package.json')).version)")

test:
	@NODE_ENV=$(NODE_ENV) \
		./node_modules/.bin/gulp test

tag:
	git push
	git tag v$(version)
	git push --tags origin master

setup:
	npm install
	npm build

deploy-staging:
	if [ `ps aux | awk '{print $2 " " $11}' | grep [m]-ashes | awk '{print $1}'` ]; then npm stop; fi
	export NODE_ENV=staging; nohup npm run dev 2>&1 &

.PHONY: test test-cov tag
