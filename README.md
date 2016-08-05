# Highlander

Highlander is the brand-spanking-new FoxCommerce mono-repo.

_This project is currently in pre-release form, so the upstream repositories
(a.k.a. the old ones) should be used for any current feature work._

## Development Environment

The simplest way to get started is to use Vagrant to build out a virtual 
machine that runs the entire system. It's really easy, though you'll
probably want to grab a coffee or take a nap the first time you run it -- it
has a lot to do!

### Install Prerequisites

- Install [Vagrant](https://www.vagrantup.com)
- Install [Ansible 1.9.x](http://docs.ansible.com/ansible/intro_installation.html#installation)

### Option A: Local Installation

If you have a good machine and a fair bit of RAM, the recommended way to work
is with the virtual machine running on your local computer. If you have limited
resources and would like to run it in Google Compute Engine, see the next step.

- Set up the machine using Vagrant

    $ vagrant up contained

- Wait - you'll have some downtime before this step finishes
- Once it completes, configure your hostfile so that you can access the system.

    $ sudo echo "192.168.10.111 local.foxcommerce.com" >> /etc/hosts
    $ sudo echo "192.168.10.111 admin.local.foxcommerce.com" >> /etc/hosts

- You're done! Navigate to the storefront on 
  [local.foxcommerce.com](http://local.foxcommerce.com) or admin on 
  [admin.local.foxcommerce.com](http://admin.local.foxcommerce.com)

### Option B: Google Compute Engine

Coming soon...

## The Projects

###[phoenix-scala](https://github.com/FoxComm/phoenix-scala)

Our main API that handles the business logic for the customer, merchandising,
and order management systems. Written in Scala.

###[green-river](https://github.com/FoxComm/green-river)

An event-sourcing system based on Kafka that utilizes
[bottledwater](https://github.com/confluentinc/bottledwater-pg) to capture all
of the changes that occur in Postgres and piple them into Kafka. It's built in
Scala and powers logging and searching capabilities in the system.

###[middlewarehouse](https://github.com/FoxComm/middlewarehouse)

A lightweight and fast shipping and inventory management service written in Go.

###[isaac](https://github.com/FoxComm/isaac)

Our C++ authentication service.

###[ashes](http://github.com/FoxComm/ashes)

The Admin UI, written in React.js.

###[api-js](https://github.com/FoxComm/api-js)

A JavaScript library for interacting with the FoxCommerce API.

###[firebrand](https://github.com/FoxComm/firebrand)

A demo storefront used to show off the capabilities of FoxCommerce APIs.

###[integration-tests](https://github.com/FoxComm/integration-tests)

Our tests for hitting the system as a black box at the API level. They test
both functionality and that every piece in the system is talking correctly to
each other.

###[prov-shit](https://github.com/FoxComm/prov-shit)

All of our DevOps tools for deploying the application to both development and
production.

## Usage

### Updating from Upstream

Updating from upstream will pull in all commits on each project's `master`
branch. All commit history in each repository will be cloned into this repo.

```
$ make update
```

## Development Environment

_TODO: Add the instructions_
