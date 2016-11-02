build:
	lein uberjar
	
docker: build
	docker build -t messaging .

docker-push: docker
	docker tag messaging docker-stage.foxcommerce.com:5000/messaging:master
	docker push docker-stage.foxcommerce.com:5000/messaging:master

test:
	true

.PHONY: build test
