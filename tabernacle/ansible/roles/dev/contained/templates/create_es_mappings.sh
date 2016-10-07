#!/bin/bash

cd /vagrant/green-river/

sbt -Denv=default -Ddefault.elastic.setup=true createMappings
