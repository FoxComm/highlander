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
| [engineering-wiki](engineering-wiki) | Internal design documents, guidelines and other tips in Markdown format.                                     |

## Development Environment

The simplest way to get started is to use Vagrant to build out a virtual
machine that runs the entire system. It's really easy, though you'll
probably want to grab a coffee or take a nap the first time you run it -- it
has a lot to do!

### Install Prerequisites

- Install [Vagrant](https://www.vagrantup.com)
- Install [Ansible 2.2.x](http://docs.ansible.com/ansible/intro_installation.html#installation)

### Google Compute VM

1. Ask one of DevOps guys for Ansible Vault password and OpenVPN keys + client configuration.

2. [Generate your SSH key](https://help.github.com/articles/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent/) for GCE and put the public key to [project metadata](https://console.cloud.google.com/compute/metadata/sshKeys?project=foxcomm-staging).

3. [Generate Google service account key](https://cloud.google.com/storage/docs/authentication#generating-a-private-key) and download it in JSON format to your machine.

4. Run `.env.local` generator, required for Vagrant. You'll be prompted for you corporate e-mail and SSH/JSON key locations.

    ```
    $ make dotenv
    ```

5. Pre-configure Vagrant by running:

    ```
    $ make prepare
    ```

6. You're ready to spin up the machine! Do it by running:

    ```
    $ make up
    ```

#### Deploying custom branch

Your generated `.env.local` will have all docker tags set to `master`:

```
export DOCKER_TAG_ASHES=master
export DOCKER_TAG_FIREBRAND=master
export DOCKER_TAG_PHOENIX=master
export DOCKER_TAG_GREENRIVER=master
# ... other project tags ...
```

If you want to spin up an appliance with custom tag for any project, please build and push it to our Docker Registry, for example:

    $ cd phoenix
    $ make build
    $ make docker
    $ DOCKER_TAG=my-custom-debug-build make docker-push

And then override `DOCKER_TAG_PHOENIX` to `my-custom-debug-build`.

TODO: Automate this process.

### Local VM

There is another option to run development environment in a local VM. Please refer to related [wiki page](engineering-wiki/development/Local-VM-DevEnv.md) for more information.
