[![Build status](https://badge.buildkite.com/ec4187f1eca5250c9d56ac493896a8daf83e7aa7db1303f4bd.svg)](https://buildkite.com/foxcommerce/phoenix)

# Phoenix

<p align="center">
  <img src="http://images2.alphacoders.com/451/451370.jpg">
</p>

## Development

`sbt '~re-start' will reload the application automatically on code changes`

### Dependencies

- [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

http://computechtips.com/781/install-oracle-jdk-8-mac-os-x-10-10-yosemite

#### [SBT](http://www.scala-sbt.org/)

OSX:

```bash
brew install sbt
```

#### [Scala](http://www.scala-lang.org/)

OSX:

```bash
brew install scala
```

#### [Flyway](http://flywaydb.org/getstarted/)

OSX:

```bash
brew install flyway
```

### Setup

```bash
make configure
```

### Using Vagrant

```bash
vagrant up
```
