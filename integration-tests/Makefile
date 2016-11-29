include ../makelib
header = $(call baseheader, $(1), integration-tests)

clean:
	sbt clean

build:
	$(call header, Building)
	sbt gatling:compile

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

.PHONY: build clean test docker docker-push
