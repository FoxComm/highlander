#!/bin/bash
MACHINE="vagrant@192.168.10.111"
KEY="../prov-shit/.vagrant/machines/appliance/virtualbox/private_key" 
cd ../green-river
 sbt assembly && scp -i $KEY target/scala-2.11/green-river-assembly-1.0.jar $MACHINE:./

 ssh -i $KEY $MACHINE 'sudo cp green-river-assembly-1.0.jar /usr/local/bin/green-river.jar && sudo systemctl restart greenriver'

