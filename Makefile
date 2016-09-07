
setup:
	npm install
	npm build

build:
	test -f .env && export eval `cat .env` || true && gulp build

package: build
	touch firebrand.tar.bz2
	tar --exclude 'firebrand.tar.bz2' -jcf firebrand.tar.bz2 ./


.PHONY: setup build package
