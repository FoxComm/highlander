#!/bin/bash
MACHINE=${MACHINE:="appliance"}
PROVIDER=${PROVIDER:="virtualbox"}

ADDRESS="vagrant@192.168.10.111"
KEY="../prov-shit/.vagrant/machines/$MACHINE/$PROVIDER/private_key"
cd ../phoenix-scala
sbt assembly && scp -i $KEY target/scala-2.11/phoenix-scala-assembly-1.0.jar $ADDRESS:./

ssh -i $KEY $ADDRESS 'sudo cp phoenix-scala-assembly-1.0.jar /usr/local/bin/phoenix.jar && sudo systemctl restart phoenix'
