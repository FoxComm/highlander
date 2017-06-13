# Ashes

### Prerequisites

* node

>= 7.0.0 is required version for Ashes.
To install this or anothers versions of node you can use [brew](http://brew.sh), [nvm](https://github.com/creationix/nvm) or [n](https://github.com/tj/n) node version manager.

If using nvm, run the following to install the version listed in `.nvmrc`. For example, if using node `v7.7.1`, follow these commands:

```
nvm install 7.7.1
nvm use 7.7.1
```

### Flow

We're using [Flow](https://flowtype.org) to gradually implement type checking in Ashes. Currently, only PIM modules are typed.

Install Flow per the instructions on the website.

### Install npm modules

```
yarn
```

### Get certificate to communicate with Phoenix

just add `DEV_SKIP_JWT_VERIFY=1` to `.env` file for non-production usage.

or you can clone `tabernacle` repository and use `tabernacle/ansible/roles/secret_keys/files/public_key.pem`. It is encrypted by ansible and you'll have to decrypt it. Add it to `.env` file: `PHOENIX_PUBLIC_KEY=${PATH_TO_KEY}`.

### Run the dev server

```
make d
```

### Pointing to Phoenix

By default, Ashes looks locally for phoenix and ElasticSearch `http://localhost`. If you want to change
which phoenix and ES Ashes uses, you can set the `API_URL` environment variable `API_URL=...` to `.env` file.

```
API_URL=https://stage.foxcommerce.com
```

### Run the production build and then server

```
make p
```

### Stripe.js

In order to Stripe.js to work (used for creating credit cards in Stripe) you need to provide publishable key for stripe (https://stripe.com/docs/stripe.js#setting-publishable-key)
You can set Stripe key by exporting `STRIPE_PUBLISHABLE_KEY` variable, or setting it in your `.env`:

  `STRIPE_PUBLISHABLE_KEY=pk_test_r6t0niqmG9OOZhhaSkacUUU1`

## Building docker image

1. Install [Docker CE for Mac](https://store.docker.com/editions/community/docker-ce-desktop-mac)
2. Build your app with `make build`
3. Create `ashes/.npmrc-docker` file and put there the result of `cat ~/.npmrc` plus `cat ./.npmrc`. Should be something like that:

```
//npm.foxcommerce.com:4873/:_authToken="SECRET_TOKEN"
@foxcomm:registry=https://npm.foxcommerce.com:4873/
```

Where `SECRET_TOKEN` – an auth token for private packages, like `@foxcomm/wings`, which was stored by npm when you did `npm add user` in `~/.npmrc` file.

4. Build docker with `make docker`

Thats it! You can run it locally: `docker run -it -p 4000:4000 ashes:latest` (or the same `make docker-run`) or push it with `make docker-push`. After having running it you can also explore it filesystem with `docker exec -t -i d138d762ff3d /bin/sh`.
