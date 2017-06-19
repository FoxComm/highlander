# Development environment Makefile
include makelib
-include goldrush.cfg
header = $(call baseheader, $(1), highlander)

prepare:
	sudo pip install -r tabernacle/requirements.txt

config:
	cd tabernacle && ansible-playbook --inventory-file=inventory/static/dev ansible/goldrush_config_gen.yml

up:
	$(call header, Creating Appliance)
	ansible-playbook -vvvv --user=$(GOOGLE_SSH_USERNAME) --private-key=$(GOOGLE_SSH_KEY) --extra-vars '{"FIRST_RUN": false}' tabernacle/ansible/goldrush_appliance.yml
	@cat goldrush.log

destroy:
	$(call header, Destroying Appliance)
	ansible-playbook tabernacle/ansible/goldrush_appliance_destroy.yml
	@rm -rf goldrush.log
	@rm -rf goldrush.state

provision:
	$(call header, Provisioning Appliance)
	ansible-playbook -v --user=$(GOOGLE_SSH_USERNAME) --private-key=$(GOOGLE_SSH_KEY) tabernacle/ansible/goldrush_appliance.yml
	@cat goldrush.log

publish:
	cd developer-portal && make publish

.PHONY: prepare config up destroy provision publish
