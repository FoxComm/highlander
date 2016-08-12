FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
FLYWAY_TEST=flyway -configFile=sql/flyway.test.conf -locations=filesystem:sql/

DB=middlewarehouse_development
DB_TEST=middlewarehouse_test
DB_USER=middlewarehouse
BUILDPATH=${GOPATH}/src/github.com/FoxComm/highlander/middlewarehouse
SUBDIRS = api common controllers models repositories routes services
TESTDIRS = $(SUBDIRS:%=test-%)

prep:
	mkdir -p ${GOPATH}/src/github.com/FoxComm/highlander
	ln -s `pwd` $(BUILDPATH); true

glide: prep
	cd $(BUILDPATH) && glide install && glide update

build: glide
	cd $(BUILDPATH) && go build -o middlewarehouse main.go
	$(MAKE) -C consumers/stock-items build

build-linux:
	GOOS=linux $(MAKE) build
	$(MAKE) -C consumers/stock-items build-linux

migrate:
	${FLYWAY} migrate

migrate-test:
	${FLYWAY_TEST} migrate

reset: drop-db drop-user create-user create-db migrate migrate-test

reset-test:
	dropdb --if-exists ${DB_TEST}
	createdb ${DB_TEST}
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
	$(MAKE) -C consumers/stock-items test

test: $(TESTDIRS) test-consumers
$(TESTDIRS): PACKAGE = $(@:test-%=%)
$(TESTDIRS):
	cd $(PACKAGE) && GOENV=test go test ./...

