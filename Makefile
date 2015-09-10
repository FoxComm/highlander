FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/

configure: resetdb

clean:
	${FLYWAY} clean

deploy-staging:
	sbt assembly
	rsync -avz ./target/scala-2.11/phoenix-scala-assembly-1.0.jar deploy@104.197.51.67:~/phoenix.jar
	rsync -avz ./resources/run_phoenix.sh ./sql deploy@104.197.51.67:~/
	ssh deploy@104.197.51.67 "export PHOENIX_ENV=staging; \
		/usr/local/share/flyway/flyway -configFile=sql/flyway.conf -locations=filesystem:sql clean"
	ssh deploy@104.197.51.67 "export PHOENIX_ENV=staging; \
		/usr/local/share/flyway/flyway -configFile=sql/flyway.conf -locations=filesystem:sql migrate && \
		java -cp phoenix.jar utils.Seeds"
	ssh deploy@104.197.51.67 "export PHOENIX_ENV=staging; nohup ./run_phoenix.sh > /dev/null 2>&1 &"

migrate:
	${FLYWAY} migrate

migrate-info:
	${FLYWAY} info

resetdb:
	dropdb --if-exists phoenix_development
	dropdb --if-exists phoenix_test
	dropuser --if-exists phoenix
	createuser -s phoenix
	createdb phoenix_development
	createdb phoenix_test
	@make migrate

.PHONY: configure clean migrate setup

