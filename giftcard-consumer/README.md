# ShipStation

The FoxCommerce ShipStation service that bridges the communication between the
FoxCommerce core platform and the ShipStation API.

## Goals

The goals of the ShipStation service are to:

- Keep ShipStation up-to-date with relevant SKU information, such as length,
  width, height, and weight.
- Notify ShipStation and middlewarehouse when an order shifts to fulfillment
  started.
- Receive API requests from ShipStation when an item has shipped, and rely those
  messages to middlewarehouse.
- Isolate Phoenix, middlewarehouse, and any future core FoxCommerce services
  from knowing any of the specifics about the ShipStation API.

## Design

This service reacts to changes in SKUs and Orders by acting as a Kafka consumer.

## Getting Started

### Prerequisites

1. Go (1.6 or greater)

  Install from [https://golang.org](https://golang.org) or from Homebrew if on
  OSX.

2. Glide

  Mac OS X:

  ```
  brew install glide
  ```

  Ubuntu:

  ```
  sudo add-apt-repository ppa:masterminds/glide && sudo apt-get update
  sudo apt-get install glide
  ```


