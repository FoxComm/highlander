configure: resetdb

clean:
	flyway -configFile=conf/flyway.conf -locations=filesystem:sql/ clean

migrate:
	flyway -configFile=conf/flyway.conf -locations=filesystem:sql/ migrate

resetdb:
	dropdb phoenix_development
	dropuser phoenix
	createuser -s phoenix
	createdb phoenix_development
	@make migrate

.PHONY: configure clean migrate setup
