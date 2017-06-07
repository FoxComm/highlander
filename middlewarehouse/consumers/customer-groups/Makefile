include ../../../makelib
header = $(call baseheader, $(1), customer-groups-consumer)

build:
	$(call header, Building)
	cd ../../ && make build-customer-groups-consumer

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	cd ../../ && make docker-customer-groups-consumer

docker-push:
	$(call header, Registering)
	cd ../../ && make push-customer-groups-consumer

.PHONY: build test true docker
