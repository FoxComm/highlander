
setup:
	npm install
	npm build

build: setup
	gulp build

package: build
	touch firebird.tar.bz2
	tar --exclude 'firebird.tar.bz2' -jcf firebird.tar.bz2 ./
	

.PHONY: setup build package
