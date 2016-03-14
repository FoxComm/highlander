FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
DB_EXISTS=test "`psql -lqt --host ${PG_HOST} --user ${PG_USER} | grep phoenix_development`"

configure: resetdb

clean:
	@${DB_EXISTS} && ${FLYWAY} clean || true

migrate:
	${FLYWAY} migrate

migrate-info:
	${FLYWAY} info

resetdb:
	dropdb --host localhost --if-exists phoenix_development
	dropdb --host localhost --if-exists phoenix_test
	dropuser --host localhost --if-exists phoenix
	createuser --host localhost -s phoenix
	createdb --host localhost phoenix_development
	createdb --host localhost phoenix_test
	@make migrate

.PHONY: configure clean migrate setup
