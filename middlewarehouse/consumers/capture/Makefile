include ../../../makelib
header = $(call baseheader, $(1), capture-consumer)

build:
	$(call header, Building)
	cd ../../ && make build-capture-consumer

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	cd ../../ && make docker-capture-consumer

docker-push:
	$(call header, Registering)
	cd ../../ && make push-capture-consumer

.PHONY: build test true docker
