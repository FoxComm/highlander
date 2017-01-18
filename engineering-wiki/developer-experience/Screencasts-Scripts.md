# Screencasts Scripts

A scripts for upcoming series of screencasts for onboarding process.

## Setting Up Personal Development Environment

```
Hello and welcome to FoxCommerce!

In this screencast we will learn how to setup a personal development environment.

The process itself - launching an instance in Google Cloud bundled with all necessary software - is simple enough, but requires some manual configuration.

You probably already have a corporate e-mail and an access to GitHub.

Go ahead and clone our main mono-repo called "Highlander":

    $ git clone git@github.com:FoxComm/highlander.git

Before starting the configuration process, you'll need to install Vagrant...

    $ vagrant --version

... and Ansible.

    $ ansible --version

Our developer appliances are running in a private network, so you'll need to contact our DevOps team for personal connection keys.

Also, it's necessary to generate your personal SSH key and download a Google Cloud service account key. You'll find a detailed guides in description to this video.

Let's kick off! Vagrant uses a lot of personal environment variables, such as corporate e-mail, SSH key location and others. Let's prepare them by running:

    $ make prepare && make dotenv

You'll be prompted for necessary information, after that, you're ready to push the final red button:

    $ make up

If everything is set correctly, Vagrant will prepare an instance which will be provisioned by Ansible.

    <vagrant logs appear>

Please note that provisioning process can take 20-30 minutes, depending on your internet connection.

    <ansible logs appear>

In sake of the length of this video, we did a dry-run instead of actual provisioning.

    <goldrush logs appear>

Your instance is ready! In next video, we will learn how to change things there.

```

## Deploying and Testing Custom Branch of Application

TBD.

Notes: short note about Mesos, running `docker build && docker push`, swapping containers in Marathon, checking for Firebrand changes. Running seeders. Link to Anna's doc.

## Using Bundled Software to Solve Problems

TBD.

Notes: showcase of third-party services we have inside appliances: Systemd, Consul, Elasticsearch, Mesos, Kibana, Pgweb.

