include ../makelib
header = $(call baseheader, $(1), phoenix-scala)

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_TAG ?= master
GIT_BRANCH ?= $(shell git symbolic-ref --short HEAD)
INSTANCE ?= local.foxcommerce.com

build:
	$(call header, Building)
	sbt 'fullAssembly'

build-phoenix:
	sbt 'phoenixScala/assembly'

build-seeder:
	sbt 'seeder/assembly'

clean:
	sbt '; phoenixScala/clean; seeder/clean'

test:
	$(call header, Testing)
	sbt '; scalafmtTestAll ; test; seedDemo'

docker:
	$(call header, Dockerizing)
	make docker-phoenix
	make docker-seeder

docker-phoenix:
	docker build -t phoenix .

docker-seeder:
	docker build -t phoenix-seeder -f seeder/Dockerfile .

docker-push:
	$(call header, Registering)
	make docker-push-phoenix
	make docker-push-seeder

docker-push-phoenix:
	docker tag phoenix $(DOCKER_REPO)/phoenix:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/phoenix:$(DOCKER_TAG)

docker-push-seeder:
	docker tag phoenix-seeder $(DOCKER_REPO)/phoenix-seeder:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/phoenix-seeder:$(DOCKER_TAG)

update-app:	
	cd ../tabernacle && ansible-playbook -v -i inventory/static/dev ansible/goldrush_update_app.yml --extra-vars "app_name=phoenix branch_name='$(GIT_BRANCH)' auto_build=y instance_ip=$(INSTANCE)"

.PHONY: build test clean docker docker-push update-app
