build:
	true

test:
	true

docker:
	docker build -t solomon .

docker-push:
	docker tag solomon docker-stage.foxcommerce.com:5000/solomon:master
	docker push docker-stage.foxcommerce.com:5000/solomon:master

.PHONY: build test
