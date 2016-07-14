FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
FLYWAY_TEST=flyway -configFile=sql/flyway.test.conf -locations=filesystem:sql/

DB=middlewarehouse_development
DB_TEST=middlewarehouse_test
DB_USER=middlewarehouse

build:
	go build main.go

migrate:
	${FLYWAY} migrate

migrate-test:
	${FLYWAY_TEST} migrate

reset: drop-db drop-user create-user create-db

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

test:
	GOENV=test go test $(shell go list ./... | grep -v /vendor/)
