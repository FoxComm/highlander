
setup:
	npm install
	npm build

build: setup
	gulp build
	touch firebrand.tar.bz2
	tar --exclude 'firebrand.tar.bz2' -jcf firebrand.tar.bz2 ./

docker: build
	docker build -t firebrand .

clean:
	rm -rf ./node_modules

test:
	npm test

.PHONY: setup build package test docker
