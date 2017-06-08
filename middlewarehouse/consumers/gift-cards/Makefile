include ../../../makelib
header = $(call baseheader, $(1), gift-card-consumer)

build:
	$(call header, Building)
	cd ../../ && make build-gift-card-consumer

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	cd ../../ && make docker-gift-card-consumer

docker-push:
	$(call header, Registering)
	cd ../../ && make push-gift-card-consumer

.PHONY: build test true docker
