#!/bin/bash
MACHINE=${MACHINE:="appliance"}
PROVIDER=${PROVIDER:="virtualbox"}

ADDRESS="vagrant@192.168.10.111"
KEY="../prov-shit/.vagrant/machines/$MACHINE/$PROVIDER/private_key"
cd ../green-river
 sbt assembly && scp -i $KEY target/scala-2.11/green-river-assembly-1.0.jar $ADDRESS:./

 ssh -i $KEY $ADDRESS 'sudo cp green-river-assembly-1.0.jar /usr/local/bin/green-river.jar && sudo systemctl restart greenriver'
