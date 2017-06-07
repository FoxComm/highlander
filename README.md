# Highlander

[![Build status](https://badge.buildkite.com/f3f7826395143bf97d4f1b11fd2fb3c78ecb2a9c71b5acb43e.svg)](https://buildkite.com/foxcommerce/highlander-master)

Highlander is the brand-spanking-new FoxCommerce mono-repo.

Please proceed to [wiki](https://github.com/FoxComm/highlander/wiki) for more detailed info.

## Development Setup
From highlander directory:
`git config core.hooksPath $PWD/utils/git-hooks`

## Development Environment

The simplest way to get started is setup a personal developer appliance in Google Cloud that runs the entire system. It's really easy, though you'll probably want to grab a coffee the first time you run it - it has a lot to do!

### Install Prerequisites

- [Python](https://python.org) 2.7.x
- [Ansible](https://ansible.com) 2.2.x

### Google Compute VM

1. Ask one of DevOps guys for Ansible Vault password and OpenVPN keys + client configuration.

2. [Generate your SSH key](https://help.github.com/articles/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent/) for GCE and put the public key to [project metadata](https://console.cloud.google.com/compute/metadata/sshKeys?project=foxcomm-staging).

3. [Generate Google service account key](https://cloud.google.com/storage/docs/authentication#generating-a-private-key) and download it in JSON format to your machine (role: `Owner`).

4. Install Ansible dependencies by running:

    ```
    $ make prepare
    ```

5. Run config generator, required for Ansible. You'll be prompted for you service account e-mail, JSON/SSH _private_ key absolute locations, and optional: custom instance name (string) and additional IC apps for analytics (True/False).

    ```
    $ make config
    ```

6. You're ready to spin up the machine! Do it by running (will take few minutes):

    ```
    $ make up
    ```

#### Deploying Custom Branches

Please refer to related [wiki page](https://github.com/FoxComm/highlander/wiki/Deploying-Custom-Branches) for more information.

## FoxCommerce Software Relationships

Logical relationships between services and software.

![alt text](documents/diagrams/system/system.dot.png "Logical Model")

Physical model.

![alt text](documents/diagrams/system/system.neato.png "Physical Model")
