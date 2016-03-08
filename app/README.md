# What is this shit?

Phoenix is a quasi-RESTful HTTP API server which forms the foundation of
FoxCommerce's transactional orders system.

## Design goals

0. **Correctness.**

  * No one gives a shit how RESTful, fast, or "dev friendly" your API is unless it works *correctly*.
  * We use type-level programming as much as possible to avoid
    unnecessary errors.
  * We use FP techniques and immutability where appropriate to write
    correct code with less effort.

1. **Data integrity: it follows from 1. but deserves special mention.**

  * We use FK/domain/uniqueness constraints and explicit transactions to ensure data integrity.
  * Data modification should run through application validation.
    functions as well as duplicated/additional Postgres check constraints.
  * We eschew common ORM approaches to polymorphism to maintain
    referential integrity. (See [this SO summary of](http://stackoverflow.com/a/922341)).
  * We understand Postgresql's transaction isolation levels and how to
    avoid the problems found in [Feral Concurrency Control: An Empirical Investigation of Modern Application Integrity](http://www.bailis.org/papers/feral-sigmod2015.pdf)

2. **Accept Postgres into your heart; it's cleverer than you are.**

  * We don't try to "abstract away" our DB. We won't swap it out with
    MongoDB or another hot-flavor-of-the-month.
  * Leverage every possible feature of Postgres possible: jsonb,
    hstore, listen/notify, GIS, CTE's, triggers, functions, custom
    domain types, row-level locking, upsert, materialized views, etc.
  * The more we get out of Postgres the less we rely on custom code
    or auxiliary persistence layers.

3. **Performance.**

  * Make it work, make it right, make it fast.
  * I heard profiling helps.

4. **A modicum of modularity.**

  * Because, microservices.

## Overview

- [Your server as a function](http://monkey.org/~marius/funsrv.pdf)
  request -> decode -> business logic -> encode -> response

### Structure

```
λ tree -d -L 1 app

app
├── concepts   <-- prototypes/interfaces

├── models     <-- Slick models (case classes) + table objects

├── payloads   <-- JSON payloads -> case classes (with validation)

├── responses  <-- JSON responses -> case classes (presenters of models)

├── routes     <-- HTTP routes organized by end-user/component + auth constraints

├── server     <-- loads config, connects to PSQL, and starts actors/HTTP server

├── services   <-- business logic

└── utils      <-- common functions/helpers/don't-know-where-to-put-shit
```

### Models (& Tables)

Read more [here](app/models/README.md).

### Payloads

A `Payload` is a `case class` which can decode incoming JSON. They are provided manually in the routing layer inside `app/routes` so that `akka-http` can unmarshal JSON and map it to the `case class`. Some of the payloads extend `Validation` so that they can be validated in service prior to being used.

### Responses

A `ResponseItem` is a `case class` which represents the API response for
a given route. Each response should `extend ResponseItem` so that we can
restrict encoding of our API responses to presenters only. This
convention helps us avoid leaking sensitive/private information to
consumers such as `Customer.password`.

### Services

Read more [here](app/services/README.md).

### Resources

- [Your server as a function](http://monkey.org/~marius/funsrv.pdf)
- [cats docs](http://non.github.io/cats//index.html)
- [herding (learning) cats](http://eed3si9n.com/herding-cats/index.html)
- [slick 3.1](http://slick.typesafe.com/doc/3.1.0/)
- [akka-http](http://doc.akka.io/docs/akka-stream-and-http-experimental/2.0.2/scala/http/index.html)
- [Feral Concurrency Control](http://www.bailis.org/papers/feral-sigmod2015.pdf)
- [SQL anti-patterns](https://pragprog.com/book/bksqla/sql-antipatterns)
- [FP in Scala](https://www.manning.com/books/functional-programming-in-scala)
