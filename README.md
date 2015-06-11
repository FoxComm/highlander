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

### Vagrant setup

#### Local
```
vagrant up --provider=virtualbox
```

### GCE Spinup
```
vagrant plugin install vagrant-google
vagrant up --provider=google
```

You should then be able to access Ashes at `localhost:5000`.
