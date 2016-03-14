FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
DB_EXISTS=test "`psql -lqt --host ${PG_HOST} --user ${PG_USER} | grep phoenix_development`"

configure: resetdb

clean:
	@${DB_EXISTS} && ${FLYWAY} clean || true

migrate:
	${FLYWAY} migrate

migrate-info:
	${FLYWAY} info

resetdb: clean
	dropdb --host ${PG_HOST} --user ${PG_USER} --if-exists phoenix_development
	dropdb --host ${PG_HOST} --user ${PG_USER} --if-exists phoenix_test
	dropuser --host ${PG_HOST} --user ${PG_USER} --if-exists phoenix
	createuser --host ${PG_HOST} --user ${PG_USER} -s phoenix
	createdb --host ${PG_HOST} --user ${PG_USER} phoenix_development
	createdb --host ${PG_HOST} --user ${PG_USER} phoenix_test
	@make migrate

.PHONY: configure clean migrate setup
