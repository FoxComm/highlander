include ../makelib
header = $(call baseheader, $(1), api-js)

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

.PHONY: build test docker docker-push
