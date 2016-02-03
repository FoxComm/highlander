# Models

Our model framework borrows heavily from ideas of ActiveRecord/RoR while
avoiding the bad parts of that software.

We use `case class` records which map to a given table in Postgres
similiar to most ORMs, except we use Slick 3 and its FRM (whatever the
fuck that is). A companion object of the same name is frequently used to
define `status` ADTs and helper functions.

Each model has 1:1 to a pluralized table class which defines column
names and types providing the shape/mapping support for Slick3 from a
table record to a `case class` model. The table companion object is
where re-usable queries are placed and/or helper logic that doesn't
belong in a service.

## Validation

The abstract Model extends `Validation` which means every model has a
`validate` method where we write custom logic similiar to ActiveRecord
to ensure data integrity. Since we're FP, we don't mutate our `case
class` models; rather, we `.copy` them to modify values which can also
be run thru the `updateTo` function for validation.

## CRUD

Our tables ensure data integrity for models when
creating/updating/changing FSM by running validations when using general
CRUD functions.

## Best Practices

- `Option` for null columns
- Don't use `Future` in model or table queries. This would belong in
  `app/services`

### Resources

- [slick 3.1](http://slick.typesafe.com/doc/3.1.0/)
