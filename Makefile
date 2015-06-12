# TODO: move me
NEXT_MIGRATION=$(shell ls sql/*.sql | sed "s/.*V\([0-9?]\).*/\1/g" | sort -nr | head -n 1 | xargs expr 1 +)

configure: resetdb

clean:
	flyway -configFile=conf/flyway.conf -locations=filesystem:sql/ clean

migrate:
	flyway -configFile=conf/flyway.conf -locations=filesystem:sql/ migrate

resetdb:
	dropdb phoenix_development || true
	dropdb phoenix_test || true
	dropuser phoenix || true
	createuser -s phoenix
	createdb phoenix_development
	createdb phoenix_test
	@make migrate

create-migration:
	$(EDITOR) sql/V$(NEXT_MIGRATION)__create_$(table)_table.sql

.PHONY: configure clean migrate setup
