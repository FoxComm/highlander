DOCKER_TAG ?= isaac
DOCKER_BRANCH ?= master

build:
	mkdir -p build
	cd build && cmake .. && make

clean:
	rm -rf ./build

test:
	true

docker:
	docker build -t $(DOCKER_TAG) .

docker-push:
	docker tag $(DOCKER_TAG) docker-stage.foxcommerce.com:5000/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push docker-stage.foxcommerce.com:5000/$(DOCKER_TAG):$(DOCKER_BRANCH)

.PHONY: build test
