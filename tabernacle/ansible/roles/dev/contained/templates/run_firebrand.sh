#!/bin/bash

export LISTEN_PORT=4041
export HOME=/root
export API_URL=http://{{api_server}}
export PHOENIX_PUBLIC_KEY={{ public_keys_dest_dir }}/public_key.pem
export FIREBIRD_LANGUAGE={{firebrand_default_language}}

export NODE_ENV=development

cd /vagrant/firebrand
npm run dev
