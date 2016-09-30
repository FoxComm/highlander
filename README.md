# Highlander

Highlander is the brand-spanking-new FoxCommerce mono-repo.

## Development Environment

The simplest way to get started is to use Vagrant to build out a virtual
machine that runs the entire system. It's really easy, though you'll
probably want to grab a coffee or take a nap the first time you run it -- it
has a lot to do!

### Install Prerequisites

- Install [Vagrant](https://www.vagrantup.com)
- Install [Ansible 1.9.x](http://docs.ansible.com/ansible/intro_installation.html#installation)

### Build the Application

The easiest way to get the entire application built is to use the Vagrant Build
environment. This will launch a VM that contains all the dependencies needed to
build all services.

**Step 1: Build the VM**

```
$ vagrant up build
```

**Step 2: Build the Services**

SSH into the build VM

    $ vagrant ssh build

Navigate to the source directory

    $ cd /vagrant

Start all of the build scripts

    $ make build

Grab a cup of coffee... this will take a while.

When everything is completed, all executables needed to build a development VM
will have been created. You can exit the VM.

### Launch a VM

The appliance VM (a single VM containing all services) can be run either on your
local environment through VirtualBox or VMWare Fusion, or in the cloud through
Google Compute Engine. If you have sufficient hardware resources, a local VM
will give you the most flexibility and performance.

**Local VM**

Provision a VM

    $ vagrant up

Set your hosts file so that you can access the site by adding the following to `/etc/hosts`

    192.168.10.111 local.foxcommerce.com

Connect to the site through your browser.

**Google Compute VM**

Install the GCE vagrant provider

    $ vagrant plugin install vagrant-google

Add the following vagrant box.

    $ vagrant box add gce https://github.com/mitchellh/vagrant-google/raw/master/google.box

Set the following environment variables.

    $ export GOOGLE_SSH_KEY=~/.ssh/google_compute_engine # Or the location of your key
    $ export GOOGLE_CLIENT_EMAIL=<Your FoxCommerce email>

Download a JSON key for our GCE environment. You can follow
[Google's instructions for generating a private key](https://cloud.google.com/storage/docs/authentication#generating-a-private-key).

Make sure to generate a JSON key. And save it with the name `foxcomm-staging.json` in the root directory of this project.

Once downloaded, set the location.

    $ export GOOGLE_JSON_KEY_LOCATION=`pwd`/foxcomm-staging.json

Then run

    $ vagrant up --provider=google

Test machines are created without a public facing IP address, so you'll need to use the VPN to access it.

Get the private IP address

    $ vagrant ssh
    $ ifconfig eth0

Edit your hosts file so that `local.foxcommerce.com` points to the new box using the private IP address you just retrieved.

## The Projects

| Name                                   | Type                                                                                                         |
|:---------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| [phoenix-scala](phoenix-scala)         | Our main API that handles the business logic for the customer, merchandising, and order management systems.  |
| [green-river](green-river)             | An event-sourcing system based on Kafka and [bottledwater](https://github.com/confluentinc/bottledwater-pg). |
| [middlewarehouse](middlewarehouse)     | A lightweight and fast shipping and inventory management service written in Go.                              |
| [isaac](isaac)                         | Our C++ authentication service.                                                                              |
| [ashes](ashes)                         | The Admin UI, written in React.js.                                                                           |
| [api-js](api-js)                       | A JavaScript library for interacting with the FoxCommerce API.                                               |
| [firebrand](firebrand)                 | A demo storefront used to show off the capabilities of FoxCommerce APIs.                                     |
| [integration-tests](integration-tests) | Our tests for hitting the system as a black box at the API level.                                            |
| [prov-shit](prov-shit)                 | All of our DevOps tools for deploying the application to both development and production.                    |

## Usage

### Updating from Upstream

Updating from upstream will pull in all commits on each project's `master`
branch. All commit history in each repository will be cloned into this repo.

```
$ make update
```

## Development Environment

_TODO: Add the instructions_
