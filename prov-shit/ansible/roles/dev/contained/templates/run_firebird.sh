#!/bin/bash

export HOME=/home/vagrant
export API_URL=http://{{api_server}}
export DEMO_USER={{demo_user}}
export DEMO_PASS={{demo_pass}}
export PHOENIX_PUBLIC_KEY={{ public_keys_dest_dir }}/public_key.pem
export FIREBIRD_LANGUAGE={{firebird_default_language}}
export NODE_ENV=development

cd /vagrant/firebird

npm run dev
