
build:
	go build -o shipstation main.go

build-linux:
	GOOS=linux $(MAKE) build

docker:
	docker build -t shipstation-consumer .

docker-push:
	docker tag shipstation-consumer docker-stage.foxcommerce.com:5000/shipstation-consumer:master
	docker push docker-stage.foxcommerce.com:5000/shipstation-consumer:master

.PHONY: build
