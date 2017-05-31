
# FoxComm Developer Portal

The FoxComm Developer Portal is published to [http://developer.foxcommerce.com](http://developer.foxcommerce.com).

## Installation

Prerequisites:

* npm

Build:

	$ make build

## Publishing
First install the [firebase CLI tools](https://github.com/firebase/firebase-tools)

    $ sudo npm install -g firebase-tools
    

Use firebase login:ci to generate a token then set the environment variable FIREBASE_TOKEN 

    $ make publish
	
## Links

* [API Blueprint Specification](https://github.com/apiaryio/api-blueprint/blob/master/API%20Blueprint%20Specification.md)
* [Aglio Documentation](https://github.com/danielgtaylor/aglio)
* [MSON Specification](https://github.com/apiaryio/mson/blob/master/MSON%20Specification.md)
