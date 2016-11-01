FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
FLYWAY_TEST=flyway -configFile=sql/flyway.test.conf -locations=filesystem:sql/

DB=middlewarehouse_development
DB_TEST=middlewarehouse_test
DB_USER=middlewarehouse
GOPATH=$(HOME)/tmpgo
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/middlewarehouse
HIGHLANDER_PATH=$(CURDIR)/..

prep:
	rm -rf $(BUILD_ROOT_PATH)
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH)

glide: prep
	cd $(BUILD_PATH) && glide install

build: glide
	cd $(BUILD_PATH) && go build -o middlewarehouse main.go
	cd $(BUILD_PATH) && go build -o consumers/shipments/shipments-consumer consumers/shipments/*.go
	cd $(BUILD_PATH) && go build -o consumers/stock-items/stock-items-consumer consumers/stock-items/*.go
	cd $(BUILD_PATH) && go build -o consumers/capture/capture-consumer consumers/capture/*.go

docker: glide
	cd $(BUILD_PATH) && GOOS=linux go build -o middlewarehouse main.go
	cd $(BUILD_PATH) && GOOS=linux go build -o consumers/shipments/shipments-consumer consumers/shipments/*.go
	cd $(BUILD_PATH) && GOOS=linux go build -o consumers/stock-items/stock-items-consumer consumers/stock-items/*.go
	cd $(BUILD_PATH) && GOOS=linux go build -o consumers/capture/capture-consumer consumers/capture/*.go
	docker build -t middlewarehouse .
	docker build -t stock-items-consumer consumers/stock-items/
	docker build -t shipments-consumer consumers/shipments/
	docker build -t capture-consumer consumers/capture/

docker-push:
	docker tag middlewarehouse docker-stage.foxcommerce.com:5000/middlewarehouse:master
	docker tag stock-items-consumer docker-stage.foxcommerce.com:5000/stock-items-consumer:master
	docker tag shipments-consumer docker-stage.foxcommerce.com:5000/shipments-consumer:master
	docker tag capture-consumer docker-stage.foxcommerce.com:5000/capture-consumer:master
	docker push docker-stage.foxcommerce.com:5000/middlewarehouse:master
	docker push docker-stage.foxcommerce.com:5000/stock-items-consumer:master
	docker push docker-stage.foxcommerce.com:5000/shipments-consumer:master
	docker push docker-stage.foxcommerce.com:5000/capture-consumer:master

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

test: glide reset-test
	# -p 1 serves to run tests sequentially, as it is required for non-isolated database tests
	cd $(BUILD_PATH) && GOENV=test go test -p 1 `glide nv`
