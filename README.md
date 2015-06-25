# Ashes

[![Build status](https://badge.buildkite.com/68cb05a9ec22487b81ecc2ab3befcd42c7648b78416a65e708.svg)](https://buildkite.com/foxcommerce/ashes)

### Prerequisites

* iojs

### Install npm modules

```
npm install
```

### Run the dev server
```
npm run dev
```
If connecting to the real phoenix backend (instead of local Node.Js mock api), run:
```
npm run phoenix
``` 

### Vagrant setup

#### Local
```
vagrant up --provider=virtualbox
```

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
