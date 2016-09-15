# Fox Messaging

Consume green river events and push notifications

## Prerequisites

- JVM
- [lein](http://leiningen.org) or [boot](http://boot-clj.com)

## Configuration

Fox-messaging uses [environ](https://github.com/weavejester/environ)

Required options

```clojure
:slack-webhook-url
:kafka-broker
:schema-registry-url
:fc-admin-url
```

You can use java system properties, environment variables and other methods to set options.
See [environ](https://github.com/weavejester/environ) for more details.

## Boot commands

- Build uber jar

```bash
make build
```
