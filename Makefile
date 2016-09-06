
setup:
	npm install
	npm build

build: setup
	export eval `cat .env` && gulp build

package: build
	touch firebrand.tar.bz2
	tar --exclude 'firebrand.tar.bz2' -jcf firebrand.tar.bz2 ./


.PHONY: setup build package
