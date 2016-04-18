# Fox messaging


Consume green river events and push notifications

## Prerequisites

- JVM
- [boot](http://boot-clj.com/)

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

- Built uber jar

```bash
boot build
```


## Tips and tricks

### Faster boot startup:

```bash
export BOOT_JVM_OPTIONS="-client -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none"
```
See more at: https://github.com/boot-clj/boot/wiki/JVM-Options

### Generate `project.clj` for third-party projects

Use `lein-generate` snippet describe below
https://github.com/boot-clj/boot/wiki/For-Cursive-Users

