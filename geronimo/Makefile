include ../makelib

header = $(call baseheader, $(1), geronimo)

FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
FLYWAY_TEST=flyway -configFile=sql/flyway.test.conf -locations=filesystem:sql/

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= geronimo
DOCKER_TAG ?= master

DB=geronimo_development
DB_TEST=geronimo_test
DB_USER=geronimo

####################################################################
# Build targets                                                    #
####################################################################

build:
	$(call header, Building)
	make prepare

prepare:
	mix local.hex --force
	mix local.rebar --force
	mix deps.get

test:
	$(call header, Testing)
	make prepare
	make reset-test
	MIX_ENV=test mix espec

seed:
	mix run priv/seeds/clothes_accessories_categories.exs
	mix run priv/seeds/clothes_schema.exs

####################################################################
# Docker build targets                                             #
####################################################################

docker:
	$(call header, Dockerizing)
	make docker-geronimo
	make docker-geronimo-seeder

docker-geronimo:
	docker build -t $(DOCKER_IMAGE) .

docker-geronimo-seeder:
	docker build -t geronimo-seeder -f Dockerfile.seed .

####################################################################
# Docker build targets                                             #
####################################################################

docker-push:
	$(call header, Registering)
	make push-geronimo
	make push-geronimo-seeder

push-geronimo-seeder:
	docker tag geronimo-seeder $(DOCKER_REPO)/geronimo-seeder:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/geronimo-seeder:$(DOCKER_TAG)

push-geronimo:
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

####################################################################
# Miscellaneous targets                                            #
####################################################################

clean:
	$(call header, Cleaning)
	mix deps.clean --all
	mix clean --all

migrate:
	$(FLYWAY) migrate

reset:
	@make drop-db
	@make drop-user
	@make create-user
	@make create-db
	@make migrate

drop-db:
	dropdb --if-exists $(DB)

create-db:
	createdb $(DB)

drop-user:
	dropuser --if-exists $(DB_USER)

create-user:
	createuser -s $(DB_USER)

reset-test:
	dropdb --if-exists $(DB_TEST) -U $(DB_USER)
	createdb $(DB_TEST) -U $(DB_USER)
	@make migrate-test

migrate-test:
	$(FLYWAY_TEST) migrate

.PHONY: build test docker docker-push
