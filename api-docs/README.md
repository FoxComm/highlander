# FoxComm API Documentation

Phoenix API documentation in [API Blueprint](https://github.com/apiaryio/api-blueprint) format.

## Installation

Prerequisites:

* [nvm](https://github.com/creationix/nvm)

Build:

	$ nvm install
	$ make install
	$ make build

## Deploy

Hosting is at Firebase static hosting, requires `firebase` cli and permissions to `stage-fox-developers` [stage] & `fox-developers` [production] projects.

1. Uncomment the appropriate included `js` in `/template/index.jade` [see important note below]
2. Run the deploy command:

	`$ yarn run deploy-stage` or  
	`$ yarn run deploy-production`

**IMPORTANT NOTE:**
Environment [stage vs production] is set dynamically by `package.json` task for core `firebase` login, but because of the way `aglio` is set up, more work has to be done to pass `NODE_ENV` through to its `jade` templates. So in the meantime it’s **very important** to set the correct included javascript in the head of `/template/index.jade` before deploying.

## Watch

Watch command will launch built-in web server with livereload on [http://localhost:3000](http://localhost:3000)

Watch Admin Endpoints:

	$ make watch

Watch Merchandising Endpoints:

    $ make watch_merchandising

Watch Transaction Endpoints:

    $ make watch_transactions

Watch Customer Endpoints:

	$ make watch_customer

Watch Public Endpoints:

	$ make watch_public

## Links

* [API Blueprint Specification](https://github.com/apiaryio/api-blueprint/blob/master/API%20Blueprint%20Specification.md)
* [Aglio Documentation](https://github.com/danielgtaylor/aglio)
* [MSON Specification](https://github.com/apiaryio/mson/blob/master/MSON%20Specification.md)
