# Green River

An event-sourcing system based on Kafka that utilizes bottledwater to capture all of the changes that occur in Postgres and pipe them into Kafka.
It's built in Scala and powers logging and searching capabilities in the system.

### tasks

All tasks should be run with env `localhost` when running green-river locally.

To create mappings run `sbt -Denv=localhost createMappings`
To start Green-river run `sbt -Denv=localhost '~re-start'`
