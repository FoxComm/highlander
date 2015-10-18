# Ashes

[![Build status](https://badge.buildkite.com/68cb05a9ec22487b81ecc2ab3befcd42c7648b78416a65e708.svg)](https://buildkite.com/foxcommerce/ashes)

### Prerequisites

* iojs

2.x.x is required version for Ashes.
Newer version is incompatible with current version of project due to dependencies used.
To install 2.x.x version you can use [nvm](https://github.com/creationix/nvm) or [n](https://github.com/tj/n) node version manager.

If using nvm, run the following to install the version listed in `.nvmrc`. For example, if using iojs `v2.5.0`, follow these commands:

```
nvm install iojs-v2.5.0
nvm use iojs-v2.5.0
```

### Install npm modules

```
npm install
```

### Run the dev server
```
npm run dev
```

By default, gulp run tests before starting node-server, but you can define env variable ASHES_NO_TEST_FOR_DEV
for disable this behaviour.

Also gulp can notify you about tasks completion if env variable ASHES_NOTIFY_ABOUT_TASKS is defined.

The default mode for watchify is polling. You can override polling interval via WATHIFY_POLL_INTERVAL env variable
or completely override watchify options via `.watchifyrc` file in project root.

### Git Hooks

If you want to setup some Git hooks, run the following:

```
./node_modules/.bin/gulp hooks
```

Now, installed hook runs tests and prevents push if they haven't passed.
If you prefer skip test run on each file change you can define env variable ASHES_NO_WATCH_FOR_TEST.

### Vagrant setup

#### Local
```
vagrant up --provider=virtualbox
```

### GCE Spinup
You need to set the following environment variables:

- GOOGLE_CLIENT_EMAIL: The email for the oauth client
- GOOGLE_JSON_KEY_LOCATION: The location of the oauth json file
- GOOGLE_SSH_USERNAME: The username for ssh to google
- GOOGLE_SSH_KEY: The location of your ssh key for google (eg. ~/.ssh/id_rsa)

```
vagrant plugin install vagrant-google
vagrant up --provider=google
```

You should then be able to access Ashes at `localhost:5000`.
