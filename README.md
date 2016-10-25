# The Perfect Gourmet

[![Build status](https://badge.buildkite.com/99167bb0d9f818e7018b5bea587dceb9c7540912eda5e4669b.svg)](https://buildkite.com/foxcommerce/the-perfect-gourmet)

The Perfect Gourmet store. Isomorphic React app powered by FoxComm's backend API.

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

  `npm run dev-stage` — backend API at `stage.foxcommerce.com`

  `npm run dev-local` — backend API at `http://localhost:9090`

1. Set Stripe.js publishable key.
In order to checkout to work you should set Stripe key by exporting `STRIPE_PUBLISHABLE_KEY` variable, or setting it in your `.env` file if you're using foreman, or run dev command with it:

  `export STRIPE_PUBLISHABLE_KEY=pk_test_r6t0niqmG9OOZhhaSkacUUU1`

  `STRIPE_PUBLISHABLE_KEY=pk_test_r6t0niqmG9OOZhhaSkacUUU1 npm run dev`


1. Develop it at http://localhost:4044/

1. Use `--browser-sync` flag to enable CSS hot reloading:

`npm run dev -- --browser-sync`, then open `http://localhost:3000`


You can set the backend API URL as a shell variable `API_URL`.

For example, to hit staging:

```
export API_URL=http://10.240.0.8
```
then run

```
npm run dev
```


## Push hooks

By default, gulpfile installs pre-push hooks for you.
And, usually, it's ok having pre-push hooks, even if you needed to push broken branch
you can push with `--no-verify` option.
Also, you can disable auto installing hooks by creating `.gulprc` in project root with following content:

```
exports.autoInstallHooks = false;
```

## Base infrastructure

For achieve right isomorphism [redux-wait](https://www.npmjs.com/package/redux-wait) is used.
It utilizes multiple rendering calls for get all async dependencies for project.
Read about code organization limitations in redux-wait's README.

For **grids** [lost](https://www.npmjs.com/package/lost) postcss plugin is used. It's really good.
For different margins which depends on viewport size use `--grid-margin` css variable: `margin: 0 var(--grid-margin)`.

For **static type checking** [flowtype](http://flowtype.org/) is used. You can run check manually by `npm run flow` command.

For **icons** svg icons is used. Just place svg icon to `src/images/svg` folder and gulp sprites task builds sprite for you
automatically. Name for each icon in a sprite will be `fc-<file-name-lowecased>` Usage:

```jsx
import Icon from 'ui/icon';

const icon = <Icon name="fc-google" />;

```

## Expand diffs for src/node_modules files in GitHub

GitHub [doesn't allow](https://github.com/github/linguist/issues/2206#issuecomment-103383178) overriding config
for vendor files in terms of diff suppression.
So, for you convenience, you can install [userscript](./unsuppressor.user.js) that expands diffs for you automatically.
You can install this userscript via [tampermonkey](http://tampermonkey.net) for chrome or
via [greasemonkey](https://addons.mozilla.org/en-US/firefox/addon/greasemonkey/) for firefox.

![Firebird and Phoenix](http://i.imgur.com/7Cyj5q8.jpg "Firebird and Phoenix")
