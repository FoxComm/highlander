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

build:
	$(call header, Building)
	make prepare
	cd $(BUILD_PATH) && go build -o middlewarehouse main.go
	# cd $(BUILD_PATH) && go build -o consumers/shipments/shipments-consumer consumers/shipments/*.go
	# cd $(BUILD_PATH) && go build -o consumers/stock-items/stock-items-consumer consumers/stock-items/*.go
	# cd $(BUILD_PATH) && go build -o consumers/gift-cards/gift-card-consumer consumers/gift-cards/*.go
	# cd $(BUILD_PATH) && go build -o consumers/capture/capture-consumer consumers/capture/*.go
	# cd $(BUILD_PATH) && go build -o consumers/shipstation/shipstation-consumer consumers/shipstation/*.go
	# cd $(BUILD_PATH) && go build -o common/db/seeds/seeder common/db/seeds/main.go

docker:
	$(call header, Dockerizing)
	cd $(BUILD_PATH) && docker build -t $(DOCKER_IMAGE) .
	# cd $(BUILD_PATH) && docker build -t stock-items-consumer -f consumers/stock-items/Dockerfile .
	# cd $(BUILD_PATH) && docker build -t shipments-consumer -f consumers/shipments/Dockerfile .
	# cd $(BUILD_PATH) && docker build -t capture-consumer -f consumers/capture/Dockerfile .
	# cd $(BUILD_PATH) && docker build -t shipstation-consumer -f consumers/shipstation/Dockerfile .
	# cd $(BUILD_PATH) && docker build -t middlewarehouse-seeder -f common/db/seeds/Dockerfile .
	# cd $(BUILD_PATH) && docker build -t gift-card-consumer -f consumers/gift-cards/Dockerfile .

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_IMAGE) $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	# docker tag stock-items-consumer $(DOCKER_REPO)/stock-items-consumer:$(DOCKER_TAG)
	# docker tag gift-card-consumer $(DOCKER_REPO)/gift-card-consumer:$(DOCKER_TAG)
	# docker tag shipments-consumer $(DOCKER_REPO)/shipments-consumer:$(DOCKER_TAG)
	# docker tag capture-consumer $(DOCKER_REPO)/capture-consumer:$(DOCKER_TAG)
	# docker tag shipstation-consumer $(DOCKER_REPO)/shipstation-consumer:$(DOCKER_TAG)
	# docker tag middlewarehouse-seeder $(DOCKER_REPO)/middlewarehouse-seeder:$(DOCKER_TAG)
	docker push $(DOCKER_REPO)/$(DOCKER_IMAGE):$(DOCKER_TAG)
	# docker push $(DOCKER_REPO)/stock-items-consumer:$(DOCKER_TAG)
	# docker push $(DOCKER_REPO)/gift-card-consumer:$(DOCKER_TAG)
	# docker push $(DOCKER_REPO)/shipments-consumer:$(DOCKER_TAG)
	# docker push $(DOCKER_REPO)/capture-consumer:$(DOCKER_TAG)
	# docker push $(DOCKER_REPO)/shipstation-consumer:$(DOCKER_TAG)
	# docker push $(DOCKER_REPO)/middlewarehouse-seeder:$(DOCKER_TAG)

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

