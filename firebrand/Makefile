
setup:
	npm install

build: setup
	test -f .env && export eval `cat .env` || true && NODE_ENV=production ./node_modules/.bin/gulp build

docker:
	docker build -t firebrand .

docker-push: docker
	docker tag firebrand docker-stage.foxcommerce.com:5000/firebrand:master
	docker push docker-stage.foxcommerce.com:5000/firebrand:master

clean:
	rm -rf ./node_modules

test: setup
	npm test

.PHONY: setup build test docker
