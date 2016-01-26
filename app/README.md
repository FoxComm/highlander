# What is this shit?

Phoenix is a quasi-RESTful HTTP API server which forms the foundation of
FoxCommerce's transactional orders system.

- slick3
- cats
- akka-http
- FP

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
    referential integrity.

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
  * I heard profiling will help here.

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

- json payload decoding + validations

### Responses

- refrain from using models

### Services

Read more [here](app/models/README.md).

### Resources

- [Your server as a function](http://monkey.org/~marius/funsrv.pdf)
- []()
