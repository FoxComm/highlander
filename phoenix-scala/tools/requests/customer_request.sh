#!/usr/bin/env bash

JWT=`curl -XPOST -i -H 'Content-Type: application/json' http://192.168.10.111/api/v1/public/login -d '{"email":"adil@adil.com", "password":"donkeycommerce", "kind":"customer"}' 2> /dev/null | grep "^JWT: " | sed 's/JWT: //'`
curl -X $1 -H "JWT: $JWT" -H 'Content-Type: application/json' http://192.168.10.111/api/$2 -d "$3"
