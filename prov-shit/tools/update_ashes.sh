#!/bin/bash
MACHINE="vagrant@192.168.10.111"
KEY=".vagrant/machines/appliance/virtualbox/private_key" 
rsync -zav -e "ssh -i $KEY" ../ashes/ $MACHINE:ashes
