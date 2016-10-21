archive = the-perfect-gourmet.tar.bz2

setup:
	npm install

build: setup
	test -f .env && export eval `cat .env` || true && ./node_modules/.bin/gulp build
	touch $(archive)
	tar --exclude '$(archive)' -jcf $(archive) ./

docker: build
	docker build --tag storefront .

clean:
	rm -rf ./node_modules

test:
	npm test

.PHONY: setup build docker clean test
