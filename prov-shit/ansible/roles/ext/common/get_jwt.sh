#!/bin/sh

# PHOENIX_URL=http://localhost:9090
# PHOENIX_USER=admin@admin.com
# PHOENIX_PASS=
# PHOENIX_ORG=


CREDS="{\"email\":\"${PHOENIX_USER}\",\"password\":\"${PHOENIX_PASS}\",\"org\":\"${PHOENIX_ORG}\"}"
export JWT=$(curl -X POST -H"Content-Type: application/json" -d$CREDS -i ${PHOENIX_URL}/v1/public/login | grep 'JWT:' | cut -d':' -f2 | cut -c 2-)

#echo $JWT

