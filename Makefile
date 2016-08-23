FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
FLYWAY_TEST=flyway -configFile=sql/flyway.test.conf -locations=filesystem:sql/

DB=middlewarehouse_development
DB_TEST=middlewarehouse_test
DB_USER=middlewarehouse

SUBDIRS = api common controllers models repositories routes services
TESTDIRS = $(SUBDIRS:%=test-%)

configure:
	glide install

build:
	go build -o middlewarehouse main.go
	go build -o consumers/shipments/shipments-consumer consumers/shipments/*.go
	go build -o consumers/stock-items/stock-items-consumer consumers/stock-items/*.go
	go build -o consumers/capture/capture-consumer consumers/capture/*.go

build-linux:
	GOOS=linux $(MAKE) build

migrate:
	${FLYWAY} migrate

migrate-test:
	${FLYWAY_TEST} migrate

reset: drop-db drop-user create-user create-db migrate migrate-test

reset-test:
	dropdb --if-exists ${DB_TEST} -U ${DB_USER}
	createdb ${DB_TEST} -U ${DB_USER}
	@make migrate-test

drop-db:
	dropdb --if-exists ${DB}
	dropdb --if-exists ${DB_TEST}

create-db:
	createdb ${DB}
	createdb ${DB_TEST}

drop-user:
	dropuser --if-exists ${DB_USER}

create-user:
	createuser -s ${DB_USER}

test-consumers:
	GOENV=test cd consumers/shipments && go test -v ./...
	GOENV=test cd consumers/stock-items && go test -v ./...
	GOENV=test cd consumers/capture && go test -v ./...

test: $(TESTDIRS)
$(TESTDIRS): PACKAGE = $(@:test-%=%)
$(TESTDIRS):
	cd $(PACKAGE) && GOENV=test go test ./...

