# Marketplace UI

[![Build status](https://badge.buildkite.com/1238dff6913c220ef0612d9a9f4b0c5198a8dd270d260f8ff2.svg)](https://buildkite.com/foxcommerce/highlander)

Marketplace UI implementation of a FoxCommerce-powered store. Isomorphic React app powered by FoxComm's backend API.

## Local Development

### Prerequisites

* `node` > v5.1.0

  To install this or another versions of node you can use [n](https://github.com/tj/n) or [nvm](https://github.com/creationix/nvm)

* [Flow](http://flowtype.org)

  We're using [Flow](http://flowtype.org) to perform type checks and `babel-plugin-typecheck` for same thing at runtime. Install Flow per the instructions on the website. Checkout required version in `.flowconfig` file.

* `public_key.pem` in the root of the project, as described in the [engineering wiki](https://github.com/FoxComm/engineering-wiki/blob/master/development/setup.md#developing-frontend-applications)


### Run the dev server

1. Run `npm i` to install dependencies.

1. Select your API backend. There are convenience tasks to run the common backend development methods, hitting backend API at either local or remote stage:

  `npm run dev` — backend API at `http://localhost:4000`

1. Develop it at http://localhost:4042/


You can set the backend API URL as a shell variable `API_URL`.

For example, to hit staging:

```
export API_URL=http://10.240.0.8
```
then run

```
npm run dev
```


## Base infrastructure

For achieve right isomorphism [redux-wait](https://www.npmjs.com/package/redux-wait) is used.
It utilizes multiple rendering calls for get all async dependencies for project.
Read about code organization limitations in redux-wait's README.

For **static type checking** [flowtype](http://flowtype.org/) is used. You can run check manually by `npm run flow` command.


## TODO
1. Update `eslint-config-airbnb`
