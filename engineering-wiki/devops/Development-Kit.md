# Development Kit

Each developer appliance has a batteries included for comfortable development.

Navigation:

* [Core software](#core-software)
* [Programming languages](#programming-languages)
* [Build tools](#build-tools)
* [Command line utilities](#command-line-utilities)

## Core software

### Tier I

Literally a spine of our platform, list of open-source applications we depend on:

* [Apache Mesos](http://mesos.apache.org) - makes our datacenters multi-tenant, allowing multiple applications to work on the same machine.
    * [Marathon](https://mesosphere.github.io/marathon) - container orchestration platform for Mesos.
    * [mesos-consul](https://github.com/CiscoCloud/mesos-consul) - Mesos to Consul bridge for service discovery.
* [Confluent Platform](https://www.confluent.io/product) - complete streaming platform for large-scale distributed environments.
    * [Apache Kafka](https://kafka.apache.org) - a distributed streaming platform.
    * [Apache Zookeeper](https://zookeeper.apache.org) - centralized service for providing distributed services synchronization.
    * [Schema Registry](https://github.com/confluentinc/schema-registry) - provides a serving layer for metadata in Kafka.
* [Consul](https://www.consul.io) - highly available and distributed service discovery.
    * [consul-template](https://github.com/hashicorp/consul-template) - generic template rendering and notifications with Consul.
* [Docker](https://www.docker.com) - software containerization platform.
* [Elasticsearch](https://www.elastic.co/products/elasticsearch) - distributed, RESTful search and analytics engine.
    * [Kibana](https://www.elastic.co/products/kibana) - data visualization system for Elasticsearch.
* [Nginx](https://www.nginx.com) - high performance load balancer, web server and reverse proxy.
* [PostgreSQL](https://www.postgresql.org) - open source object-relational database system.
    * [bottledwater-pg](https://github.com/confluentinc/bottledwater-pg) - data capture system from PostgreSQL into Kafka.
    * [pgweb](https://github.com/sosedoff/pgweb) - web-based database browser for PostgreSQL.
* [rsyslog](http://www.rsyslog.com) - open-source software log-forwarding system.

### Tier II

Available only on production environments or separate instances:

* [Docker Registry](https://docs.docker.com/registry) - Docker images storage and distribution system.
* [OpenVPN](https://openvpn.net/index.php/open-source.html) - open source SSL VPN solution.
* [Sinopia](https://github.com/rlidwka/sinopia) - private NPM repository server.
* [consul-alerts](https://github.com/AcalephStorage/consul-alerts) - a simple daemon to send notifications based on Consul health checks.
* [marathon-alerts](https://github.com/ashwanthkumar/marathon-alerts) - tool for monitoring the apps running on Marathon.

## Programming languages

Pre-installed compilers and runtimes, in case you need to build or test something inside a VM:

* Oracle JVM 1.8
* [Go](https://golang.org) 1.7.4
* [Node.js](https://nodejs.org) 7.1.0
* [Elixir](http://elixir-lang.org) 1.1.0

## Build tools

Various package management and build systems which are necessary for modern development:

* Scala
    * [sbt](http://www.scala-sbt.org) - interactive build tool for Scala.
* Go
    * [glide](https://github.com/Masterminds/glide) - package management for Golang.
* Clojure
    * [lein](https://github.com/technomancy/leiningen) - automating Clojure projects without setting your hair on fire.
    * [boot](https://github.com/boot-clj/boot) - Clojure build framework and ad-hoc Clojure script evaluator.
* JavaScript
    * [babel-cli](https://github.com/babel/babel) - command-line compiler for writing next generation JavaScript.
    * [flow-bin](https://github.com/flowtype/flow-bin) - Binary wrapper for Flow - a static type checker for JavaScript.
    * [gulp](https://github.com/gulpjs/gulp) - streaming build system for Node.js.
    * [yarn](https://yarnpkg.com) - fast, reliable, and secure dependency management for Node.js.

## Command line utilities

Baked in command-line utilities which ease problem solving:

* [consulate](https://github.com/gmr/consulate) - command-line client for the Consul HTTP API.
* [docker-compose](https://github.com/docker/compose) - define and run multi-container applications with Docker.
* [flyway](https://flywaydb.org) - open-source database migration tool.
* [httpie](https://httpie.org) - a command line HTTP client that will make you smile.
* [jq](https://stedolan.github.io/jq) - lightweight and flexible command-line JSON processor.
* [kt](https://github.com/fgeller/kt) - commandline tool for Apache Kafka.
* [ncdu](https://dev.yorhel.nl/ncdu) - disk usage analyzer with an ncurses interface.
* [pgcli](https://github.com/dbcli/pgcli) - Postgres CLI with autocompletion and syntax highlighting.
* [zookeepercli](https://github.com/outbrain/zookeepercli) - simple, lightweight, dependable CLI for Zookeeper.
