# Development Environment

The simplest way to get started is to use Vagrant to build out a virtual
machine that runs the entire system. It's really easy, though you'll
probably want to grab a coffee or take a nap the first time you run it -- it
has a lot to do!

### Install Prerequisites

- Install [Vagrant](https://www.vagrantup.com)
- Install [Ansible 2.2.x](http://docs.ansible.com/ansible/intro_installation.html#installation)

### Build the Application

The easiest way to get the entire application built is to use the Vagrant Build
environment. This will launch a VM that contains all the dependencies needed to
build all services.

**Step 1: Build the VM**

    $ vagrant up build

**Step 2: Build the Services**

SSH into the build VM

    $ vagrant ssh build

Navigate to the source directory

    $ cd /vagrant

Start all of the build scripts

    $ make -f Makefile.ci build

Grab a cup of coffee... this will take a while.

When everything is completed, all executables needed to build a development VM
will have been created. You can exit the VM.

### Launch a VM

The appliance VM (a single VM containing all services) can be run either on your
local environment through VirtualBox or VMWare Fusion, or in the cloud through
Google Compute Engine. If you have sufficient hardware resources, a local VM
will give you the most flexibility and performance. Currently, the VM is
configured to use 8 GB of memory and 4 vCPUs, **so it’s not worth trying unless
you have 12+ GB of RAM**.

#### Local VM

Provision a VM

    $ vagrant up

Set your hosts file so that you can access the site by adding the following to `/etc/hosts`

    192.168.10.111 local.foxcommerce.com

Connect to the site through your browser.
