include ../makelib
header = $(call baseheader, $(1), api-docs)

all: clean install build

install:
	$(call header, Installing)
	true

build:
	$(call header, Building)
	true

clean:
	$(call header, Cleaning)
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

.PHONY: install build clean test
