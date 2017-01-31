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

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_IMAGE ?= middlewarehouse
DOCKER_TAG ?= master

prepare:
	rm $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true
	cd $(BUILD_PATH) && glide install
	rm -rf $(BUILD_PATH)/vendor/github.com/FoxComm/highlander

####################################################################
# Build targets                                                    #
####################################################################
build:
	$(call header, Building)
	make prepare
	make build-middlewarehouse
	make build-middlewarehouse-seeder
	make build-capture-consumer
	make build-gift-card-consumer
	make build-shipments-consumer
	make build-shipstation-consumer
	make build-stock-items-consumer

build-middlewarehouse:
	cd $(BUILD_PATH) && go build -o middlewarehouse main.go

build-middlewarehouse-seeder:
	cd $(BUILD_PATH) && go build -o common/db/seeds/seeder common/db/seeds/main.go

build-capture-consumer:
	cd $(BUILD_PATH) && go build -o consumers/capture/capture-consumer consumers/capture/*.go

build-gift-card-consumer:
	cd $(BUILD_PATH) && go build -o consumers/gift-cards/gift-card-consumer consumers/gift-cards/*.go

build-shipments-consumer:
	cd $(BUILD_PATH) && go build -o consumers/shipments/shipments-consumer consumers/shipments/*.go

build-shipstation-consumer:
	cd $(BUILD_PATH) && go build -o consumers/shipstation/shipstation-consumer consumers/shipstation/*.go

build-stock-items-consumer:
	cd $(BUILD_PATH) && go build -o consumers/stock-items/stock-items-consumer consumers/stock-items/*.go

build-customer-groups-consumer:
	cd $(BUILD_PATH) && go build -o consumers/customer-groups/customer-groups-consumer consumers/customer-groups/*.go

####################################################################
# Docker build targets                                             #
####################################################################
docker:
	$(call header, Dockerizing)
	make docker-middlewarehouse
	make docker-middlewarehouse-seeder
	make docker-capture-consumer
	make docker-gift-card-consumer
	make docker-shipments-consumer
	make docker-shipstation-consumer
	make docker-stock-items-consumer
	make docker-customer-groups-consumer

docker-middlewarehouse:
	cd $(BUILD_PATH) && docker build -t $(DOCKER_IMAGE) .

docker-middlewarehouse-seeder:
	cd $(BUILD_PATH) && docker build -t middlewarehouse-seeder -f common/db/seeds/Dockerfile .

docker-capture-consumer:
	cd $(BUILD_PATH) && docker build -t capture-consumer -f consumers/capture/Dockerfile .

docker-gift-card-consumer:
	cd $(BUILD_PATH) && docker build -t gift-card-consumer -f consumers/gift-cards/Dockerfile .

docker-shipments-consumer:
	cd $(BUILD_PATH) && docker build -t shipments-consumer -f consumers/shipments/Dockerfile .

docker-shipstation-consumer:
	cd $(BUILD_PATH) && docker build -t shipstation-consumer -f consumers/shipstation/Dockerfile .

docker-stock-items-consumer:
	cd $(BUILD_PATH) && docker build -t stock-items-consumer -f consumers/stock-items/Dockerfile .

docker-customer-groups-consumer:
	cd $(BUILD_PATH) && docker build -t customer-groups-consumer -f consumers/customer-groups/Dockerfile .

####################################################################
# Docker push targets                                              #
####################################################################
docker-push:
	$(call header, Registering)
	make push-middlewarehouse
	make push-middlewarehouse-seeder
	make push-capture-consumer
	make push-gift-card-consumer
	make push-shipments-consumer
	make push-shipstation-consumer
	make push-stock-items-consumer
	make push-customer-groups-consumer

push-middlewarehouse:
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)

push-middlewarehouse-seeder:
	docker tag middlewarehouse-seeder $(DOCKER_REPO)/middlewarehouse-seeder:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/middlewarehouse-seeder:$(DOCKER_TAG)

push-capture-consumer:
	docker tag capture-consumer $(DOCKER_REPO)/capture-consumer:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/capture-consumer:$(DOCKER_TAG)

push-gift-card-consumer:
	docker tag gift-card-consumer $(DOCKER_REPO)/gift-card-consumer:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/gift-card-consumer:$(DOCKER_TAG)

push-shipments-consumer:
	docker tag shipments-consumer $(DOCKER_REPO)/shipments-consumer:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/shipments-consumer:$(DOCKER_TAG)

push-shipstation-consumer:
	docker tag shipstation-consumer $(DOCKER_REPO)/shipstation-consumer:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/shipstation-consumer:$(DOCKER_TAG)

push-stock-items-consumer:
	docker tag stock-items-consumer $(DOCKER_REPO)/stock-items-consumer:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/stock-items-consumer:$(DOCKER_TAG)

push-customer-groups-consumer:
	docker tag customer-groups-consumer $(DOCKER_REPO)/customer-groups-consumer:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/customer-groups-consumer:$(DOCKER_TAG)

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
	# -p 1 serves to run tests sequentially, as it is required for non-isolated database tests
	cd $(BUILD_PATH) && GOENV=test go test -p 1 `glide nv`
