# Development environment Makefile
include makelib
header = $(call baseheader, $(1), root)

prepare:
	vagrant plugin install vagrant-google
	vagrant box add --force gce https://github.com/mitchellh/vagrant-google/raw/master/google.box

dotenv:
	cd prov-shit && ansible-playbook --inventory-file=bin/envs/dev ansible/goldrush_env_local.yml

up:
	$(call header, Creating GCE Machine)
	export eval `cat ./.env.local`; vagrant up --provider=google appliance

provision:
	$(call header, Provisioning GCE Machine)
	export eval `cat ./.env.local`; VAGRANT_DEFAULT_PROVIDER=google vagrant provision appliance

destroy:
	$(call header, Destroying GCE Machine)
	export eval `cat ./.env.local`; VAGRANT_DEFAULT_PROVIDER=google vagrant destroy appliance --force

ssh:
	$(call header, Connecting to GCE Machine)
	export eval `cat ./.env.local`; VAGRANT_DEFAULT_PROVIDER=google vagrant ssh appliance

.PHONY: up provision destroy ssh
