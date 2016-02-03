#!/bin/bash
cd ../../
MACHINE="vagrant@192.168.10.111"
KEY="prov-shit/.vagrant/machines/appliance/virtualbox/private_key" 
rsync -zav -e "ssh -i $KEY" ashes/ $MACHINE:ashes
ssh -i $KEY $MACHINE 'sudo systemctl restart ashes'
