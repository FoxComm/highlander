include ../makelib
header = $(call baseheader, $(1), middlewarehouse)

FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
FLYWAY_TEST=flyway -configFile=sql/flyway.test.conf -locations=filesystem:sql/

DB=middlewarehouse_development
DB_TEST=middlewarehouse_test
DB_USER=middlewarehouse
GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/middlewarehouse
HIGHLANDER_PATH=$(CURDIR)/..

CMD_PREFIX=cd $(BUILD_PATH) && GOPATH=$(GOPATH)
GO=$(CMD_PREFIX) go
GO_TEST=$(CMD_PREFIX) GOENV=test go test
GLIDE=$(CMD_PREFIX) glide
DOCKER=$(CMD_PREFIX) docker

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= middlewarehouse
DOCKER_TAG ?= master

prepare:
	rm -rf $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true
	$(GLIDE) install
	cd $(BUILD_PATH)/elasticmanager && GOPATH=$(GOPATH) glide install
	rm -rf $(BUILD_PATH)/vendor/github.com/FoxComm/highlander

####################################################################
# Build targets                                                    #
####################################################################
build:
	$(call header, Building)
	make prepare
	make build-middlewarehouse

build-all:
	$(call header, Building All)
	make prepare
	make build-middlewarehouse
	make build-elasticmanager
	make build-middlewarehouse-seeder
	make build-capture-consumer
	make build-gift-card-consumer
	make build-shipments-consumer
	make build-shipstation-consumer
	make build-customer-groups-consumer

build-middlewarehouse:
	$(GO) build -o middlewarehouse main.go

build-elasticmanager:
	$(GO) build -o elasticmanager/elasticmanager elasticmanager/main.go

build-middlewarehouse-seeder:
	$(GO) build -o common/db/seeds/seeder common/db/seeds/main.go

build-capture-consumer:
	$(GO) build -o consumers/capture/capture-consumer consumers/capture/*.go

build-gift-card-consumer:
	$(GO) build -o consumers/gift-cards/gift-card-consumer consumers/gift-cards/*.go

build-shipments-consumer:
	$(GO) build -o consumers/shipments/shipments-consumer consumers/shipments/*.go

build-shipstation-consumer:
	$(GO) build -o consumers/shipstation/shipstation-consumer consumers/shipstation/*.go

build-customer-groups-consumer:
	$(GO) build -o consumers/customer-groups/customer-groups-consumer consumers/customer-groups/*.go

####################################################################
# Docker build targets                                             #
####################################################################
docker:
	$(call header, Dockerizing)
	make docker-middlewarehouse

docker-all:
	$(call header, Dockerizing All)
	make docker-middlewarehouse
	make docker-middlewarehouse-seeder
	make docker-capture-consumer
	make docker-gift-card-consumer
	make docker-shipments-consumer
	make docker-shipstation-consumer
	make docker-customer-groups-consumer
	make docker-elasticmanager

docker-middlewarehouse:
	$(DOCKER) build -t $(DOCKER_IMAGE) .

docker-middlewarehouse-seeder:
	$(DOCKER) build -t middlewarehouse-seeder -f Dockerfile.seeder .

docker-capture-consumer:
	$(DOCKER) build -t capture-consumer -f Dockerfile.capture .

docker-gift-card-consumer:
	$(DOCKER) build -t gift-card-consumer -f Dockerfile.gift-cards .

docker-shipments-consumer:
	$(DOCKER) build -t shipments-consumer -f Dockerfile.shipments .

docker-shipstation-consumer:
	$(DOCKER) build -t shipstation-consumer -f Dockerfile.shipstation .

docker-customer-groups-consumer:
	$(DOCKER) build -t customer-groups-consumer -f Dockerfile.customer-groups .

docker-elasticmanager:
	true

####################################################################
# Docker push targets                                              #
####################################################################
docker-push:
	$(call header, Registering)
	make push-middlewarehouse

docker-push-all:
	$(call header, Registering All)
	make push-middlewarehouse
	make push-middlewarehouse-seeder
	make push-capture-consumer
	make push-gift-card-consumer
	make push-shipments-consumer
	make push-shipstation-consumer
	make push-customer-groups-consumer
	make push-elasticmanager

push-middlewarehouse:
	$(DOCKER) tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	$(DOCKER) push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

push-middlewarehouse-seeder:
	$(DOCKER) tag middlewarehouse-seeder $(DOCKER_REPO)/middlewarehouse-seeder:$(DOCKER_TAG)
	$(DOCKER) push $(DOCKER_REPO)/middlewarehouse-seeder:$(DOCKER_TAG)

push-capture-consumer:
	$(DOCKER) tag capture-consumer $(DOCKER_REPO)/capture-consumer:$(DOCKER_TAG)
	$(DOCKER) push $(DOCKER_REPO)/capture-consumer:$(DOCKER_TAG)

push-gift-card-consumer:
	$(DOCKER) tag gift-card-consumer $(DOCKER_REPO)/gift-card-consumer:$(DOCKER_TAG)
	$(DOCKER) push $(DOCKER_REPO)/gift-card-consumer:$(DOCKER_TAG)

push-shipments-consumer:
	$(DOCKER) tag shipments-consumer $(DOCKER_REPO)/shipments-consumer:$(DOCKER_TAG)
	$(DOCKER) push $(DOCKER_REPO)/shipments-consumer:$(DOCKER_TAG)

push-shipstation-consumer:
	$(DOCKER) tag shipstation-consumer $(DOCKER_REPO)/shipstation-consumer:$(DOCKER_TAG)
	$(DOCKER) push $(DOCKER_REPO)/shipstation-consumer:$(DOCKER_TAG)

push-customer-groups-consumer:
	$(DOCKER) tag customer-groups-consumer $(DOCKER_REPO)/customer-groups-consumer:$(DOCKER_TAG)
	$(DOCKER) push $(DOCKER_REPO)/customer-groups-consumer:$(DOCKER_TAG)

push-elasticmanager:
	true

####################################################################
# Miscellaneous targets                                            #
####################################################################
clean:
	rm -rf ./vendor

migrate:
	$(FLYWAY) migrate

migrate-test:
	$(FLYWAY_TEST) migrate

reset: drop-db drop-user create-user create-db migrate

seed: reset
	./seeder

reset-test:
	dropdb --if-exists $(DB_TEST) -U $(DB_USER)
	createdb $(DB_TEST) -U $(DB_USER)
	@make migrate-test

drop-db:
	dropdb --if-exists $(DB)
	dropdb --if-exists $(DB_TEST)

create-db:
	createdb $(DB)
	createdb $(DB_TEST)

drop-user:
	dropuser --if-exists $(DB_USER)

create-user:
	createuser -s $(DB_USER)

test:
	$(call header, Testing)
	make prepare
	make reset-test
	make test-only

test-only:
	# -p 1 serves to run tests sequentially, as it is required for non-isolated database tests
	$(GO_TEST) -p 1 $(shell $(GO) list ./... | grep -v /vendor/)
