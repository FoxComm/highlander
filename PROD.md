Prod Small
===================
The current process to setup the prod small environment. This can obviously 
be cleaned up and automated more.

Run terraform to create the resources. Some intsances will fail because there is
no accessible vpn. The vpn instance should be started.

get the vpn ip and create an inventory file. Run ansible/boostrap_prod_small_vpn.yml
to configure the vpn. 

SSH to the vpn machine and add a key for yourself in /etc/openvpn/keys

Once you have vpn access, run terraform again to provision the rest of the resources.

Run ansible/boostrap_prod_small_db.yml to configure the db.
