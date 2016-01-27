# Integration tests

Home for our integration (mainly between [Phoenix](https://github.com/FoxComm/phoenix-scala) and [Green River](https://github.com/FoxComm/green-river)) and stress tests.

## Simulations

* `com.foxcommerce.CustomerSimulation`

## Running

To run all simulations:

    $ sbt test

To run all simulations in specific environment (see [application.conf](src/test/resources/application.conf)):

    $ sbt -Denv=localhost test

## Links

* [Gatling Cheat Sheet](http://gatling.io/#/cheat-sheet/2.1.7)
* [Gatling's SBT Plugin Demo](https://github.com/gatling/gatling-sbt-plugin-demo)