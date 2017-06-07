include ../../../makelib
header = $(call baseheader, $(1), stock-items-consumer)

build:
	$(call header, Building)
	cd ../../ && make build-stock-items-consumer

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	cd ../../ && make docker-stock-items-consumer

docker-push:
	$(call header, Registering)
	cd ../../ && make push-stock-items-consumer

.PHONY: build test true docker
