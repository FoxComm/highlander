
setup:
	npm install

build:
	test -f .env && export eval `cat .env` || true && ./node_modules/.bin/gulp build

build-production:
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ./node_modules/.bin/gulp build

package: build
	touch marketplace-ui.tar.bz2
	tar --exclude 'marketplace-ui.tar.bz2' -jcf marketplace-ui.tar.bz2 ./

package-production: build-production
	touch marketplace-ui.tar.bz2
	tar --exclude 'marketplace-ui.tar.bz2' -jcf marketplace-ui.tar.bz2 ./

.PHONY: setup build package
