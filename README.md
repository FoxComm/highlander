# prov-shit

### Dependencies

- [Ansible](http://docs.ansible.com/ansible/intro_installation.html#installation)
- [ansible-lint](https://github.com/willthames/ansible-lint#setup)
- [Go 1.5](https://golang.org/doc/install)

### Setup

- Checkout [phoenix-scala](https://github.com/FoxComm/phoenix-scala)
- Checkout [green-river](https://github.com/FoxComm/green-river)
- Build all projects:

    ```
    $ cd phoenix-scala
    $ sbt assembly
    $ cd ../green-river
    $ sbt assembly
    $ cd ../prov-shit
    $ make build
    ```

### Lint ansible scripts

    $ make lint

### Vagrant

#### If you want to run an appliance with the backend and ashes all on one machine.

    $ vagrant up

This will bring up a machine on ip 192.168.10.111 with everything installed.

#### If you want to run just the backend

    $ vagrant up backend

This will bring up a machine on 192.168.10.111 with just the db, phoenix and green river installed.


#### If you  want to run just ashes

    $ vagrant up ashes

This will bring up a machine on 192.168.10.112 with ashes installed.
By default it looks for the backed at '192.168.10.111'. If you want to
change this you can set the environtment variable 'BACKEND_HOST'.

export BACKEND_HOST=10.240.0.8

#### If you want to run in GCE

Add the following vagrant box.

    $ vagrant box add gce https://github.com/mitchellh/vagrant-google/raw/master/google.box

Set the following environment variables.

    GOOGLE_SSH_USERNAME
    GOOGLE_SSH_KEY

    GOOGLE_CLIENT_EMAIL
    GOOGLE_JSON_KEY_LOCATION

Then run

    $ vagrant up --provider=google
    
### Update Green River assembly

You can pass green river `*.jar` file without re-provisioning.

Just run:

    $ tools/update_greenriver.sh

or:

    $ MACHINE=backend PROVIDER=vmware_fusion tools/update_greenriver.sh    
