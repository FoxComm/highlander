
setup:
	npm install

build: setup
	test -f .env && source .env || true && ./node_modules/.bin/gulp build

build-production:
	test -f .env && source .env || true && NODE_ENV=production ./node_modules/.bin/gulp build

package: build
	touch marketplace-ui.tar.bz2
	tar --exclude 'marketplace-ui.tar.bz2' -jcf marketplace-ui.tar.bz2 ./

package-production: build-production
	touch marketplace-ui.tar.bz2
	tar --exclude 'marketplace-ui.tar.bz2' -jcf marketplace-ui.tar.bz2 ./

docker:
	docker build -t marketplace-ui .

docker-push:
	docker tag marketplace-ui docker-stage.foxcommerce.com:5000/marketplace-ui:master
	docker push docker-stage.foxcommerce.com:5000/marketplace-ui:master

.PHONY: setup build package docker docker-push
