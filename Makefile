# Development environment Makefile
include makelib
header = $(call baseheader, $(1), root)

prepare:
	pip install dnsimple
	vagrant plugin install vagrant-google
	vagrant box add --force gce https://github.com/mitchellh/vagrant-google/raw/master/google.box

dotenv:
	cd prov-shit && ansible-playbook --inventory-file=bin/envs/dev ansible/goldrush_env_local.yml

up:
	$(call header, Creating GCE Machine)
	export eval `cat ./.env.local`; vagrant up --provider=google appliance
	cat goldrush.log

status:
	export eval `cat ./.env.local`; vagrant status appliance

provision:
	$(call header, Provisioning GCE Machine)
	export eval `cat ./.env.local`; vagrant provision appliance

destroy:
	$(call header, Destroying GCE Machine)
	export eval `cat ./.env.local`; vagrant destroy appliance --force

ssh:
	$(call header, Connecting to GCE Machine)
	export eval `cat ./.env.local`; vagrant ssh appliance

.PHONY: status prepare dotenv up provision destroy ssh
