# Green River

An event-sourcing system based on Kafka that utilizes bottledwater to capture all of the changes that occur in Postgres and pipe them into Kafka.
It's built in Scala and powers logging and searching capabilities in the system.

## Running

	$ sbt -Denv=localhost consume

## Vagrant

You can use vagrant to have a ready to run system with all dependencies installed.

1. Make sure your ashes directory is lowercase 'ashes' and not 'Ashes'

2. Make sure you have [phoenix-scala](https://github.com/FoxComm/phoenix-scala) and [green-river](https://github.com/FoxComm/green-river) checked out

3. Checkout the [Provisioning Repository](https://github.com/FoxComm/prov-shit) at the same directory level as ashes.

  _Ashes can be run through either a VirtualBox or VMWare Fusion provider._

  ```
  $ cd prov-shit
  $ vagrant up
  ```
