# Highlander

[![Build status](https://badge.buildkite.com/9194ecb4f86c089e8962db23843a00662dac85e98418697dd4.svg)](https://buildkite.com/foxcommerce/developer-appliance-gce)

Highlander is the brand-spanking-new FoxCommerce mono-repo.

## The Projects

| Project                              | Description                                                                                                  |
|:-------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| [phoenix-scala](phoenix-scala)       | Our main API that handles the business logic for the customer, merchandising, and order management systems.  |
| [green-river](green-river)           | An event-sourcing system based on Kafka and [bottledwater](https://github.com/confluentinc/bottledwater-pg). |
| [middlewarehouse](middlewarehouse)   | A lightweight and fast shipping and inventory management service written in Go.                              |
| [isaac](isaac)                       | Our C++ authentication service.                                                                              |
| [solomon](solomon)                   | A microservice that handles scopes, claims, roles and permissions, written in Elixir.                        |
| [messaging](messaging)               | Kafka consumer that handles e-mail notifications through Mailchimp, written in Clojure.                      |
| [ashes](ashes)                       | The Admin UI, written in React.js.                                                                           |
| [api-js](api-js)                     | A JavaScript library for interacting with the FoxCommerce API.                                               |
| [firebrand](firebrand)               | A demo storefront used to show off the capabilities of FoxCommerce APIs.                                     |
| [prov-shit](prov-shit)               | All of our DevOps tools for deploying the application to both development and production.                    |
| [api-docs](api-docs)                 | Our API documentation in API Blueprint format and Postman query collections.                                 |
| [enginnering-wiki](enginnering-wiki) | Internal design documents, guidelines and other tips in Markdown format.                                     |

## Development Environment

The simplest way to get started is to use Vagrant to build out a virtual
machine that runs the entire system. It's really easy, though you'll
probably want to grab a coffee or take a nap the first time you run it -- it
has a lot to do!

### Install Prerequisites

- Install [Vagrant](https://www.vagrantup.com)
- Install [Ansible 2.2.x](http://docs.ansible.com/ansible/intro_installation.html#installation)

### Google Compute VM

1. [Generate your SSH key for GCE](https://help.github.com/articles/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent/) and put the public key to [project metadata](https://console.cloud.google.com/compute/metadata/sshKeys?project=foxcomm-staging).

2. [Generate Google service account key](https://cloud.google.com/storage/docs/authentication#generating-a-private-key) and download it in JSON format to your machine.

3. Run `.env.local` generator, required for Vagrant. You'll be prompted for you corporate e-mail and SSH/JSON key locations.

    ```
    $ make dotenv
    ```

4. Pre-configure Vagrant by running:

    ```
    $ make prepare
    ```

5. You're ready to spin up the machine! Do it by running:

    ```
    $ make up
    ```

Test machines are created without a public facing IP address, so you'll need to use the VPN to access it.

Get the private IP address

    $ vagrant ssh
    $ ifconfig eth0

Edit your hosts file so that `local.foxcommerce.com` points to the new box using the private IP address you just retrieved.

### Local VM

There is another option to run development environment in a local VM. Please refer to related [wiki page](engineering-wiki/development/Local-VM-DevEnv.md) for more information.
