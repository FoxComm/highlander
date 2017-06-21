include ../../../../makelib
header = $(call baseheader, $(1), middlewarehouse-seeder)

build:
	$(call header, Building)
	cd ../../../ && make build-middlewarehouse-seeder

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	cd ../../../ && make docker-middlewarehouse-seeder

docker-push:
	$(call header, Registering)
	cd ../../../ && make push-middlewarehouse-seeder

.PHONY: build test true docker
