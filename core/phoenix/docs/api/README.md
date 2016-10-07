# FoxComm API Documentation

Phoenix API documentation in [API Blueprint](https://github.com/apiaryio/api-blueprint) format.

## Installation

Prerequisites:

* [nvm](https://github.com/creationix/nvm)

Build:

	$ nvm install
	$ make install
	$ make build
	
## Watch

Watch command will launch built-in web server with livereload on [http://localhost:3000](http://localhost:3000)
	
Watch Admin Endpoints:

	$ make watch
	
Watch Customer Endpoints:

	$ make watch_customer
	
Watch Public Endpoints:

	$ make watch_public

## Links

* [API Blueprint Specification](https://github.com/apiaryio/api-blueprint/blob/master/API%20Blueprint%20Specification.md)
* [Aglio Documentation](https://github.com/danielgtaylor/aglio)
* [MSON Specification](https://github.com/apiaryio/mson/blob/master/MSON%20Specification.md)
