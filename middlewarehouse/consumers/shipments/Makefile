include ../../../makelib
header = $(call baseheader, $(1), shipments-consumer)

build:
	$(call header, Building)
	cd ../../ && make build-shipments-consumer

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	cd ../../ && make docker-shipments-consumer

docker-push:
	$(call header, Registering)
	cd ../../ && make push-shipments-consumer

.PHONY: build test true docker
