## Goals

The goals of this consumer are to:

- Find orders that contains giftCards
- Call Phoenix-scala to create giftcards
## Design

This service reacts to changes in Orders by acting as a Kafka consumer.

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
