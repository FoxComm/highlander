include ../makelib
header = $(call baseheader, $(1), tabernacle)

GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

# Main commands
all: build

build:
	$(call header, Building)
	true

test:
	$(call header, Testing)
	true

deploy-stage:
	ansible-playbook -v -i inventory/gce/ ansible/test_stage.yml

deploy-groups:
	ansible-playbook -v -i inventory/gce/ ansible/provision_groups.yml

deploy-demo1:
	ansible-playbook -v -i inventory/gce/ ansible/demo.yml --extra-vars='{ "first_run": true, "application_hostname": "demo1" }'

deploy-pure-demo:
	ansible-playbook -v -i inventory/static/dev ansible/deploy_storefront.yml

deploy-td-stage:
	ansible-playbook -v -i inventory/static/topdrawer ansible/deploy_storefront.yml

deploy-tpg-stage:
	ansible-playbook -v -i inventory/static/tpg ansible/deploy_storefront.yml

deploy-feature-branch:
	ansible-playbook -v -i inventory/gce/ ansible/deploy_feature_branch.yml

deploy-feature-branch-legacy:
	ansible-playbook -v -i inventory/gce/ ansible/deploy_feature_branch_legacy.yml

update-demo1:
	ansible-playbook -v -i inventory/gce/ ansible/demo.yml --extra-vars='{ "with_data_import": false, "application_hostname": "demo1" }'

deploy-demo2:
	ansible-playbook -v -i inventory/gce/ ansible/demo.yml --extra-vars='{ "first_run": true, "application_hostname": "demo2" }'

update-demo2:
	ansible-playbook -v -i inventory/gce/ ansible/demo.yml --extra-vars='{ "with_data_import": false, "application_hostname": "demo2" }'

docker:
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

clean:
	true

.PHONY: all build clean test docker docker-push
