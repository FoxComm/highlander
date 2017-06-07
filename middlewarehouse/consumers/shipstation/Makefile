include ../../../makelib
header = $(call baseheader, $(1), shipstation-consumer)

build:
	$(call header, Building)
	cd ../../ && make build-shipstation-consumer

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	cd ../../ && make docker-shipstation-consumer

docker-push:
	$(call header, Registering)
	cd ../../ && make push-shipstation-consumer

.PHONY: build test true docker
