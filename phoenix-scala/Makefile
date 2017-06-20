include ../makelib
header = $(call baseheader, $(1), phoenix-scala)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_TAG ?= master
GIT_BRANCH ?= $(shell git symbolic-ref --short HEAD)
INSTANCE ?= $(shell cat ../goldrush.state)

build:
	$(call header, Building)
	make build-phoenix

fmt:
	../utils/scalafmt/scalafmt.sh --diff

fmt-check:
	../utils/scalafmt/scalafmt.sh --test

build-phoenix: fmt-check
	sbt 'phoenix/assembly'

build-seeder: fmt-check
	sbt 'seeder/assembly'

clean:
	sbt '; clean; phoenix/clean; seeder/clean'

test:
	$(call header, Testing)
	sbt '; test; seedDemo'

docker:
	$(call header, Dockerizing)
	make docker-phoenix

docker-phoenix:
	docker build -t phoenix .

docker-seeder:
	docker build -t phoenix-seeder -f seeder/Dockerfile .

docker-push:
	$(call header, Registering)
	make docker-push-phoenix

docker-push-phoenix:
	docker tag phoenix $(DOCKER_REPO)/phoenix:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/phoenix:$(DOCKER_TAG)

docker-push-seeder:
	docker tag phoenix-seeder $(DOCKER_REPO)/phoenix-seeder:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/phoenix-seeder:$(DOCKER_TAG)

update-app:
	cd ../tabernacle && ansible-playbook -v -i inventory/static/dev ansible/goldrush_update_app.yml --extra-vars "app_name=phoenix branch_name='$(GIT_BRANCH)' auto_build=y instance_ip=$(INSTANCE)"

.PHONY: build test clean docker docker-push update-app fmt-check fmt
