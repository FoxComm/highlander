FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
FLYWAY_TEST=flyway -configFile=sql/flyway.test.conf -locations=filesystem:sql/

DB=middlewarehouse_development
DB_TEST=middlewarehouse_test
DB_USER=middlewarehouse
BUILDPATH=$(GOPATH)/src/github.com/FoxComm/highlander/middlewarehouse

prep:
	rm -rf $(GOPATH)/src/github.com/FoxComm/highlander
	mkdir -p $(GOPATH)/src/github.com/FoxComm/highlander
	cp -R . $(BUILDPATH)

glide: prep
	cd $(BUILDPATH) && glide install

build: glide
	cd $(BUILDPATH) && go build -o middlewarehouse main.go
	cd $(BUILDPATH) && go build -o consumers/shipments/shipments-consumer consumers/shipments/*.go
	cd $(BUILDPATH) && go build -o consumers/stock-items/stock-items-consumer consumers/stock-items/*.go
	cd $(BUILDPATH) && go build -o consumers/capture/capture-consumer consumers/capture/*.go
	cp $(BUILDPATH)/middlewarehouse ./
	cp $(BUILDPATH)/consumers/shipments/shipments-consumer ./consumers/shipments
	cp $(BUILDPATH)/consumers/stock-items/stock-items-consumer ./consumers/stock-items
	cp $(BUILDPATH)/consumers/capture/capture-consumer ./consumers/capture

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

test: reset-test
	cd $(BUILDPATH) && GOENV=test go test `glide nv`
