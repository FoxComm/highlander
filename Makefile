# Development environment Makefile
include makelib
include .env.local
header = $(call baseheader, $(1), root)

prepare:
	sudo pip install -r prov-shit/requirements.txt

dotenv:
	cd prov-shit && ansible-playbook --inventory-file=bin/envs/dev ansible/goldrush_env_local.yml

up:
	$(call header, Creating GCE Machine)
	ansible-playbook prov-shit/ansible/goldrush_appliance.yml
	@cat goldrush.log

destroy:
	$(call header, Destroying GCE Machine)
	ansible-playbook prov-shit/ansible/goldrush_appliance_destroy.yml
	@rm -rf goldrush.log
	@rm -rf goldrush.state

update-app:
	cd prov-shit && ansible-playbook -v -i bin/envs/dev ansible/goldrush_update_app.yml

# Legacy commands
migrate: prepare
	@awk '{ printf "\nexport GOOGLE_INSTANCE_NAME=%s", $$1 }' .vagrant/machines/appliance/google/id >> .env.local

provision: up

.PHONY: up migrate provision destroy update-app dotenv prepare clean
