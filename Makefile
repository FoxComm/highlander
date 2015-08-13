FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
VERSION := `git rev-parse HEAD`

configure: resetdb

clean:
	${FLYWAY} clean

deploy-staging:
	sbt assembly
	rsync -avz ./target/scala-2.11/phoenix-scala-assembly-1.0.jar deploy@104.197.85.54:~/phoenix.jar
	rsync -avz ./resources/run_phoenix.sh deploy@104.197.85.54:~/
	ssh deploy@104.197.85.54 "nohup ./run_phoenix.sh > /dev/null 2>&1 &"

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

