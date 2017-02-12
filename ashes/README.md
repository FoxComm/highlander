# Ashes

### Prerequisites

* node

5.1.0 or above is required version for Ashes.
To install this or anothers versions of node you can use [brew](http://brew.sh), [nvm](https://github.com/creationix/nvm) or [n](https://github.com/tj/n) node version manager.

If using nvm, run the following to install the version listed in `.nvmrc`. For example, if using node `v5.1.0`, follow these commands:

```
nvm install 5.1.0
nvm use 5.1.0
```

For installing prerequisites for ubuntu:

```
./install_ubuntu.sh
```

For installing prerequisites for mac os x:

```
./install_osx.sh
```

### Install Flow

We're using [Flow](https://flowtype.org) to gradually implement type checking in Ashes. Currently, only PIM modules are typed.

Install Flow per the instructions on the website.

### Install npm modules

```
npm install
```

### Get certificate to communicate with Phoenix

You can clone `prov-shit` repository and use `${PROV_SHIT_HOME}/ansible/roles/secret_keys/files/public_key.pem`. It is encrypted by ansible and you'll have to decrypt it. Add it to `.env` file: `export PHOENIX_PUBLIC_KEY=${PATH_TO_KEY}`.

or just add `export DEV_SKIP_JWT_VERIFY=1` to `.env` file for non-production usage.

### Run the dev server

```
make d
```

By default, gulp run tests before starting node-server, but you can define env variable `ASHES_NO_TEST_FOR_DEV`
for disable this behaviour. Also, see `ASHES_NO_WATCH_FOR_TEST`:

```
# disable watching of test files
export ASHES_NO_WATCH_FOR_TEST=1
# disable all tests while developing
export ASHES_NO_TEST_FOR_DEV=1
```

Also gulp can notify you about tasks completion if env variable ASHES_NOTIFY_ABOUT_TASKS is defined.

The default mode for watchify is polling. You can override polling interval via WATCHIFY_POLL_INTERVAL env variable
or completely override watchify options via `.watchifyrc` file in project root.

### Pointing to Phoenix

By default, Ashes looks locally for phoenix and ElasticSearch `http://localhost`. If you want to change
which phoenix and ES Ashes uses, you can set the `API_URL` environment variable `export API_URL=...` to `.env` file.

```
export API_URL=http://10.240.0.3
npm run dev
```

### Run the production server

```
make p
```

### Stripe.js

In order to Stripe.js to work (used for creating credit cards in Stripe) you need to provide publishable key for stripe (https://stripe.com/docs/stripe.js#setting-publishable-key)
You can set Stripe key by exporting `STRIPE_PUBLISHABLE_KEY` variable, or setting it in your `.env`:

  `export STRIPE_PUBLISHABLE_KEY=pk_test_r6t0niqmG9OOZhhaSkacUUU1`

### Git Hooks

If you want to setup some Git hooks, run the following:

```
make hooks
```

Now, installed hook runs tests and prevents push if they haven't passed.
If you prefer skip test run on each file change you can define env variable `ASHES_NO_WATCH_FOR_TEST`.

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
