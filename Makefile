FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
DB_EXISTS=test "`psql -lqt | cut -d \| -f 1 | grep -w phoenix_development | wc -l`" = "1"

configure: resetdb

clean:
	@${DB_EXISTS} && ${FLYWAY} clean || true

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

