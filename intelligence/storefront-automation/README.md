# Storefront Automation
## Setup the server
To download the selenium standalone server, run `make configure-server`.
To start the server, run `make run-server`.
This will allow selenium tests to be run at `http://localhost:4444/wd/hub`.

## Run the Agent
Currently the agent is run with `node lib/test.js`
