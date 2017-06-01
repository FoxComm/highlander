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

deploy-groups:
	ansible-playbook -v -i inventory/gce/ ansible/deploy_groups.yml

deploy-feature-branch:
	ansible-playbook -v -i inventory/gce/ ansible/deploy_feature_branch.yml

# Instances with old base images / outdated playbooks etc.
deploy-demo1:
	ansible-playbook -v -i inventory/gce/ ansible/legacy_demo.yml --extra-vars='{ "first_run": true, "application_hostname": "demo1" }'

update-demo1:
	ansible-playbook -v -i inventory/gce/ ansible/legacy_demo.yml --extra-vars='{ "with_data_import": false, "application_hostname": "demo1" }'

deploy-demo2:
	ansible-playbook -v -i inventory/gce/ ansible/legacy_demo.yml --extra-vars='{ "first_run": true, "application_hostname": "demo2" }'

update-demo2:
	ansible-playbook -v -i inventory/gce/ ansible/legacy_demo.yml --extra-vars='{ "with_data_import": false, "application_hostname": "demo2" }'

deploy-pure-demo:
	ansible-playbook -v -i inventory/static/dev ansible/legacy_deploy_storefront.yml

deploy-td-stage:
	ansible-playbook -v -i inventory/static/topdrawer ansible/legacy_deploy_storefront.yml

deploy-tpg-stage:
	ansible-playbook -v -i inventory/static/tpg ansible/legacy_deploy_storefront.yml

deploy-stage:
	ansible-playbook -v -i inventory/gce/ ansible/legacy_test_stage.yml

deploy-feature-branch-legacy:
	ansible-playbook -v -i inventory/gce/ ansible/legacy_deploy_feature_branch.yml

docker:
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

clean:
	true

.PHONY: all build clean test docker docker-push
