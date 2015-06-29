FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/

configure: resetdb

clean:
	${FLYWAY} clean

migrate:
	${FLYWAY} migrate

migrate-info:
	${FLYWAY} info

resetdb:
	dropdb phoenix_development || true
	dropdb phoenix_test || true
	dropuser phoenix || true
	createuser -s phoenix
	createdb phoenix_development
	createdb phoenix_test
	@make migrate

.PHONY: configure clean migrate setup

