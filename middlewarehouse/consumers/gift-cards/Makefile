include ../../../makelib
header = $(call baseheader, $(1), gift-cards-consumer)

build:
	$(call header, Building)
	cd ../../ && make build-gift-cards-consumer

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	cd ../../ && make docker-gift-cards-consumer

docker-push:
	$(call header, Registering)
	cd ../../ && make push-gift-cards-consumer

.PHONY: build test true docker
