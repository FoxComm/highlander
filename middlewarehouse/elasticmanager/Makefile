include ../../makelib
header = $(call baseheader, $(1), elasticmanager)

build:
	$(call header, Building)
	cd ../ && make build-elasticmanager

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	cd ../ && make docker-elasticmanager

docker-push:
	$(call header, Registering)
	cd ../ && make push-elasticmanager

.PHONY: build test true docker
