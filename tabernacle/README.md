# Tabernacle

### Docs

- [Build and Deploy for Infrastructure](docs/build_and_deploy.md)
- [How To Update Base Images](docs/base_images.md)
- [Production Environment From Scratch](docs/production.md)

### Dependencies

- [Ansible](http://docs.ansible.com/ansible/intro_installation.html#installation) (1.9.x)
- [ansible-lint](https://github.com/willthames/ansible-lint#setup) (2.2.x)
- [Go](https://golang.org/doc/install) (1.5)
- [gcloud](https://cloud.google.com/sdk/gcloud)

### Lint ansible scripts

    $ make lint

#### You will need to create `.vault_pass` file with vault password

You will need this if you run any terraform or packer command.
Get it from a co-worker as it isn't stored anywhere

