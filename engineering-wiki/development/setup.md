# Development

You need some pre-requisites to get going. It's a good idea to make sure you've installed:

#### For frontend
* **node** via any method: nodejs.org or `brew`, or node version managers `nvm` or `n`
  * also make sure you have `npm`, should come with the above
* **ansible** `brew install ansible`

#### For backend
* **go** `brew install golang`
* **vagrant** https://www.vagrantup.com/downloads.html
* **gcloud cli** https://cloud.google.com/sdk/
* [Dependencies as outlined for Phoenix](https://github.com/FoxComm/phoenix-scala#running-locally)


#### For Both

* Ask @jmataya to get access to VPN [required to connect to various non-public domains]
* Ask @jmataya for a `.vault_press` file to put in the [provisioning repo](https://github.com/FoxComm/prov-shit)


## Developing Frontend Applications

The easiest way to develop the frontend applications is to connect to the backend in the Stage environment.

1. Setup dev public key
  1. Check out [provisioning repo](https://github.com/FoxComm/prov-shit) [make sure you have `.vault_press` file as above]
  1. Copy `prov-shit/ansible/roles/base/secret_keys/files/public_key.pem` into the root of the frontend repo you are working on
  1. From the frontend repo, run `ansible-vault decrypt public_key.pem`
1. `npm install` the repo you're working on
1. `npm dev-stage` should now work!


## Developing Backend Services

There are a number of ways to run the backend services.

* [Run the whole backend ecosystem](https://github.com/FoxComm/prov-shit#vagrant)
* [Run just Phoenix](https://github.com/FoxComm/phoenix-scala#development)
