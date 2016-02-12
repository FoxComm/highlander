# Ashes

[![Build status](https://badge.buildkite.com/68cb05a9ec22487b81ecc2ab3befcd42c7648b78416a65e708.svg)](https://buildkite.com/foxcommerce/ashes)

### Prerequisites

* node

5.1.0 or above is required version for Ashes.
To install this or anothers versions of node you can use [nvm](https://github.com/creationix/nvm) or [n](https://github.com/tj/n) node version manager.

If using nvm, run the following to install the version listed in `.nvmrc`. For example, if using node `v5.1.0`, follow these commands:

```
nvm install 5.1.0
nvm use 5.1.0
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

The default mode for watchify is polling. You can override polling interval via WATCHIFY_POLL_INTERVAL env variable
or completely override watchify options via `.watchifyrc` file in project root.

### Pointing to Phoenix

By default, Ashes looks locally for phoenix at `http://localhost:9090`. If you want to change
which phoenix server Ashes uses, you can set the `PHOENIX_URL` environment variable.

```
export PHOENIX_URL=http://10.240.0.3:9090
npm run dev
```

### Git Hooks

If you want to setup some Git hooks, run the following:

```
./node_modules/.bin/gulp hooks
```

Now, installed hook runs tests and prevents push if they haven't passed.
If you prefer skip test run on each file change you can define env variable ASHES_NO_WATCH_FOR_TEST.

### Vagrant setup

#### Local

1. Make sure your ashes directory is lowercase 'ashes' and not 'Ashes'

2. Make sure you have [phoenix-scala](https://github.com/FoxComm/phoenix-scala) and [green-river](https://github.com/FoxComm/green-river) checked out

3. Checkout the [Provisioning Repository](https://github.com/FoxComm/prov-shit) at the same 
   directory level as ashes.


  _Ashes can be run through either a VirtualBox or VMWare Fusion provider._

  ```
  cd prov-shit
  vagrant up
  ```

4. Access Ashes at http://192.168.10.111.

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
