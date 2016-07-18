FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
FLYWAY_TEST=flyway -configFile=sql/flyway.test.conf -locations=filesystem:sql/

build:
	go build main.go

migrate:
	${FLYWAY} migrate

migrate-test:
	${FLYWAY_TEST} migrate

resetdb:
	dropdb --if-exists middlewarehouse_development
	dropdb --if-exists middlewarehouse_test
	dropuser --if-exists middlewarehouse
	createuser -s middlewarehouse
	createdb middlewarehouse_development
	createdb middlewarehouse_test
	@make migrate

reset-test:
	dropdb --if-exists middlewarehouse_test
	createdb middlewarehouse_test
	@make migrate-test


test:
	GOENV=test go test $(shell go list ./... | grep -v /vendor/)
