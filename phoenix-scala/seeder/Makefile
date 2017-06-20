include ../../makelib
header = $(call baseheader, $(1), phoenix-seeder)

build:
	$(call header, Building)
	cd ../ && make build-seeder

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	cd ../ && make docker-seeder

docker-push:
	$(call header, Registering)
	cd ../ && make docker-push-seeder

.PHONY: build test true docker
