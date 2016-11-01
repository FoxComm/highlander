archive = the-perfect-gourmet.tar.bz2

setup:
	npm install

build: setup
	test -f .env && export eval `cat .env` || true && ./node_modules/.bin/gulp build
	touch $(archive)
	tar --exclude '$(archive)' -jcf $(archive) ./

docker:
	docker build -t tpg-storefront .

docker-push:
	docker tag tpg-storefront docker-stage.foxcommerce.com:5000/tpg-storefront:master
	docker push docker-stage.foxcommerce.com:5000/tpg-storefront:master

clean:
	rm -rf ./node_modules

test:
	npm test

.PHONY: setup build docker docker-push clean test
