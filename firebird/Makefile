
setup:
	npm install
	npm build

build: setup
	gulp build
	touch firebird.tar.bz2
	tar --exclude 'firebird.tar.bz2' -jcf firebird.tar.bz2 ./

clean:
	rm -rf ./node_modules

test:
	npm test

.PHONY: setup build package test
