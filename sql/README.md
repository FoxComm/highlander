# SQL

We use Postgresql's features heavily and try to avoid too much
abstraction over top of the RDBMS. We use [Flyway](https://flywaydb.org/) to manage
migrations in `sql/*.sql` files. Additionally, some data is seeded in those files
(such as countries/regions) and are named accordingly.

We make use of the following features out of Postgres:

- triggers
  - to generate PK ids for concrete super table approach ([example](https://github.com/FoxComm/phoenix-scala/blob/master/sql/V20.01__create_skus.sql#L9-L20))
  - Roll-up computations which are easier/make more sense in Postgres
    rather than app code.

- mviews
  - refreshing mviews pumps explicit data views thru green-river for
    searching thru ES.
  - json aggregation

- check/domain constraints
  - duplicate code is acceptable in both the app/DB to ensure data
    integrity.
  - domains are used for custom column types such as `currency`
  - check constraints imposed on `status` or `state` columns similiar
    to enum but avoids its flaws.
  - validation
  - uniqueness constraints (better here than in the application)

- concrete super table for polymorphism for RI
  - rather than use ActiveRecord/RoR/ORM style polymorphism, this
    pattern is employed to maintain referential integrity.

- listen/notify
  - PSQL can do pub/sub and we use akka actors as the perfect fit to
    execute simple logic upon an event.
