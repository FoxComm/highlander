default: test
NODE_ENV = test
CFLAGS = -c -g -D $(NODE_ENV)

# Get version number from package.json, need this for tagging.
version = $(shell iojs -e "console.log(JSON.parse(require('fs').readFileSync('package.json')).version)")

clean:
	rm -rf ./node_modules

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

stop:
	if [ `ps aux | awk '{print $2 " " $11}' | grep [m]-ashes | awk '{print $1}'` ]; then npm stop; fi

run-staging: setup stop
	export NODE_ENV=staging; nohup npm run dev 2>&1 &

run-production: setup stop
	export NODE_ENV=production; nohup ./node_modules/.bin/gulp server 2>&1 &

pre-build: setup
	test -f .env && export eval `cat .env` || true && ./node_modules/.bin/gulp build

build-production:
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ./node_modules/.bin/gulp build

build: pre-build
	touch ashes.tar.bz2
	tar --exclude 'ashes.tar.bz2' --exclude '.git' --exclude '.gitignore' --exclude '.gitattributes' -jcf ashes.tar.bz2 ./

docker: build-production
	docker build -t ashes .

package-production: build-production
	touch ashes.tar.bz2
	tar --exclude 'ashes.tar.bz2' --exclude '.git' --exclude '.gitignore' --exclude '.gitattributes' -jcf ashes.tar.bz2 ./

.PHONY: test test-cov tag build build-production package package-production docker
