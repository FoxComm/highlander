FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/

configure: resetdb

clean:
	${FLYWAY} clean

migrate:
	${FLYWAY} migrate

migrate-info:
	${FLYWAY} info

resetdb: clean
	dropdb --if-exists phoenix_development
	dropdb --if-exists phoenix_test
	dropuser --if-exists phoenix
	createuser -s phoenix
	createdb phoenix_development
	createdb phoenix_test
	@make migrate

.PHONY: configure clean migrate setup

