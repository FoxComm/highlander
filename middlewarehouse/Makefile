include ../makelib
header = $(call baseheader, $(1), middlewarehouse)

FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
FLYWAY_TEST=flyway -configFile=sql/flyway.test.conf -locations=filesystem:sql/

DB=middlewarehouse_development
DB_TEST=middlewarehouse_test
DB_USER=middlewarehouse
GOPATH=/tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/middlewarehouse
HIGHLANDER_PATH=$(CURDIR)/..

DOCKER_REPO ?= $(DOCKER_STAGE_REPO)
DOCKER_TAG ?= middlewarehouse
DOCKER_BRANCH ?= master

prep:
	rm $(BUILD_ROOT_PATH) || true
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true

glide: prep
	cd $(BUILD_PATH) && glide install

build:
	$(call header, Building)
	make glide
	cd $(BUILD_PATH) && go build -o middlewarehouse main.go
	cd $(BUILD_PATH) && go build -o consumers/shipments/shipments-consumer consumers/shipments/*.go
	cd $(BUILD_PATH) && go build -o consumers/stock-items/stock-items-consumer consumers/stock-items/*.go
	cd $(BUILD_PATH) && go build -o consumers/gift-cards/gift-card-consumer consumers/gift-cards/*.go
	cd $(BUILD_PATH) && go build -o consumers/capture/capture-consumer consumers/capture/*.go
	cd $(BUILD_PATH) && go build -o consumers/shipstation/shipstation-consumer consumers/shipstation/*.go
	cd $(BUILD_PATH) && go build -o common/db/seeds/seeder common/db/seeds/main.go

docker:
	$(call header, Dockerizing)
	docker build -t $(DOCKER_TAG) .
	docker build -t stock-items-consumer consumers/stock-items/
	docker build -t shipments-consumer consumers/shipments/
	docker build -t capture-consumer consumers/capture/
	docker build -t shipstation-consumer consumers/shipstation/
	docker build -t middlewarehouse-seeder common/db/seeds/
	docker build -t gift-card-consumer consumers/gift-cards/

docker-push:
	$(call header, Registering)
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker tag stock-items-consumer $(DOCKER_REPO)/stock-items-consumer:$(DOCKER_BRANCH)
	docker tag gift-card-consumer $(DOCKER_REPO)/gift-card-consumer:$(DOCKER_BRANCH)
	docker tag shipments-consumer $(DOCKER_REPO)/shipments-consumer:$(DOCKER_BRANCH)
	docker tag capture-consumer $(DOCKER_REPO)/capture-consumer:$(DOCKER_BRANCH)
	docker tag shipstation-consumer $(DOCKER_REPO)/shipstation-consumer:$(DOCKER_BRANCH)
	docker tag middlewarehouse-seeder $(DOCKER_REPO)/middlewarehouse-seeder:$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/stock-items-consumer:$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/gift-card-consumer:$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/shipments-consumer:$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/capture-consumer:$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/shipstation-consumer:$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/middlewarehouse-seeder:$(DOCKER_BRANCH)

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
	make glide
	make reset-test
	# -p 1 serves to run tests sequentially, as it is required for non-isolated database tests
	cd $(BUILD_PATH) && GOENV=test go test -p 1 `glide nv`

test-local:
	make reset-test
	GOENV=test go test -p 1 `glide nv`

