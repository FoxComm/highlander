# Integration tests

Home for our integration (mainly between [Phoenix](https://github.com/FoxComm/phoenix-scala) and [Green River](https://github.com/FoxComm/green-river)) and stress tests.

## Simulations

* `com.foxcommerce.ActivitySimulation` - TBD
* `com.foxcommerce.NotificationSimulation` - TBD
* `com.foxcommerce.TrailSimulation` - materialized views synchronization simulation. Covered entities:
    * `Customer`
    * `CustomerAddress`
    * `Order`
    * `OrderShippingAddress`
    * `GiftCard`
    * `StoreCredit`

## Running

To run all simulations:

    $ sbt test

To run a single simulation:

    $ sbt testOnly com.foxcommerce.TrailSimulation

Configuration options:

* `env` - override environment, see details in [application.conf](src/test/resources/application.conf) (default: `vagrant`)
* `users` - number of users injected per simulation (default: 1)
* `pause` - pause (in seconds) between Green River synchronization (default: 7)

Example:

    $ sbt -Denv=localhost -Dusers=5 -Dpause=1 test

## Links

* [Gatling Cheat Sheet](http://gatling.io/#/cheat-sheet/2.1.7)
* [Gatling's SBT Plugin Demo](https://github.com/gatling/gatling-sbt-plugin-demo)