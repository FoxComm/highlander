include ../makelib

header = $(call baseheader, $(1), hyperion)

FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= hyperion
DOCKER_TAG ?= master

DB=hyperion_development
DB_USER=hyperion
ALIAS_IP=203.0.113.1

####################################################################
# Build targets                                                    #
####################################################################

build:
	$(call header, Building)
	make prepare

prepare:
	$(call header, Building)
	mix local.hex --force
	mix deps.get
	
test:
	$(call header, Testing)
	true

seed:
	mix run priv/seeds/clothes_accessories_categories.exs

####################################################################
# Docker build targets                                             #
####################################################################

docker:
	$(call header, Dockerizing)
	make docker-hyperion
	make docker-hyperion-seeder

docker-hyperion:
	docker build -t $(DOCKER_IMAGE) .

docker-hyperion-seeder:
	docker build -t hyperion_seeder -f Dockerfile.seed --build-arg db_host=$(ALIAS_IP) .

####################################################################
# Docker build targets                                             #
####################################################################

docker-push:
	make push-hyperion
	make push-hyperion-seeder

push-hyperion-seeder:
	docker tag hyperion_seeder $(DOCKER_REPO)/hyperion_seeder:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/hyperion_seeder:$(DOCKER_TAG)

push-hyperion:
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

####################################################################
# Miscellaneous targets                                            #
####################################################################

clean:
	mix deps.clean --all
	mix clean --all

migrate:
	$(FLYWAY) migrate

reset: drop-db drop-user create-user create-db migrate

drop-db:
	dropdb --if-exists $(DB)

create-db:
	createdb $(DB)

drop-user:
	dropuser --if-exists $(DB_USER)

create-user:
	createuser -s $(DB_USER)

.PHONY: build test docker docker-push

