# Screencasts Scripts

A scripts for upcoming series of screencasts for onboarding process.

## Setting Up Personal Development Environment

```
Hello and welcome to FoxCommerce!

In this screencast we will learn how to set up a personal development environment.

The process itself - launching an instance in Google Cloud bundled with all necessary software - is simple enough, but requires some manual configuration.

You probably already have a corporate e-mail and an access to GitHub.

Go ahead and clone our main mono-repo called "Highlander":

    $ git clone git@github.com:FoxComm/highlander.git

Before starting the configuration process, you'll need to install VirtualBox, Vagrant...

    $ vboxmanage --version
    $ vagrant --version

... and Ansible.

    $ ansible --version

Our developer appliances are running in a virtual private network, so in order to connect there you'll need to contact our DevOps team for connection keys and client configuration.

Also, it's necessary to generate your personal SSH key and download a Google Cloud service account key. You'll find a detailed guides in description to this video.

Let's kick off! Vagrant uses a lot of environment variables, so let's prepare them by running:

    $ make prepare && make dotenv

You'll be prompted for various information like key location. After that, you're ready to push the final red button:

    $ make up

If everything is set correctly, Vagrant will prepare an instance which will be provisioned by Ansible.

    <vagrant logs appear>

Please note that provisioning process can take 20-30 minutes, depending on your internet connection.

    <ansible logs appear>

In sake of the length of this video, we did a dry-run instead of actual provisioning.

    <goldrush logs appear>

Your instance is ready! Let's try to connect there:

    $ make ssh
    $ ifconfig ens4
    $ exit

In the next video, we will learn how to actually change things there.
```

## Deploying and Testing Custom Branch of Application

TBD.

Plans:

    * Opening created appliance in web browser.
    * Short note about Mesos workers.
    * Running `docker build && docker push`.
    * Swapping containers in Marathon.
    * Checking for Firebrand / Ashes for changes.
    * Mentioning Anna's doc for more info.

## Using Bundled Software to Solve Problems

TBD.

Showcase of third-party services we have inside appliances:

    * Systemd
    * Consul
    * Elasticsearch
    * Mesos
    * Kibana
    * Pgweb
    * Batteries inside like `scala`, `pgcli`, `go`, `jq`, etc.
