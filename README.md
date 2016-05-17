# Green River

[![Build status](https://badge.buildkite.com/6460bcd8efc0c0aac5bbe8cc9317f9cce92c69bc0a8675c1c6.svg)](https://buildkite.com/foxcommerce/green-river)

Green river currently represents consumers that capture [Phoenix](https://github.com/FoxComm/phoenix-scala) data changes and save them to ElasticSearch.

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
