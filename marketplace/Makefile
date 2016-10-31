build:
	true

test:
	true

docker:
	docker build -t marketplace .

docker-push:
	docker tag marketplace docker-stage.foxcommerce.com:5000/marketplace:master
	docker push docker-stage.foxcommerce.com:5000/marketplace:master

.PHONY: build test
