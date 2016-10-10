archive = the-perfect-gourmet.tar.bz2

setup:
	npm install

build:
	test -f .env && export eval `cat .env` || true && ./node_modules/.bin/gulp build

build-production:
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ./node_modules/.bin/gulp build

package: build
	touch $(archive)
	tar --exclude '$(archive)' -jcf $(archive) ./

package-production: build-production
	touch the-perfect-gourmet.tar.bz2
	tar --exclude '$(archive)' -jcf $(archive) ./

.PHONY: setup build build-production package package-production
