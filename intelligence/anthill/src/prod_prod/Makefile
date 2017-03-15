include ../../../makelib
header = $(call baseheader, $(1), prod-prod)

build:
	$(call header, Building)
	true

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

clean:
	$(call header, Cleaning)
	true

.PHONY: build test docker docker-push
