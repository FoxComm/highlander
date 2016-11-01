
build:
	mkdir -p build
	cd build && cmake .. && make

clean:
	rm -rf ./build

test:
	true

docker:
	docker build -t isaac .

docker-push:
	docker tag isaac docker-stage.foxcommerce.com:5000/isaac:master
	docker push docker-stage.foxcommerce.com:5000/isaac:master

.PHONY: build test
