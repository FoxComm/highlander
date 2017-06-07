include ../../../makelib
header = $(call baseheader, $(1), capture-consumer)

build:
	cd ../../ && make build-capture-consumer

test:
	true

docker:
	cd ../../ && make docker-capture-consumer

docker-push:
	cd ../../ && make push-capture-consumer

.PHONY: build test true docker
