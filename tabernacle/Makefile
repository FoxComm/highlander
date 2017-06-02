include ../makelib
header = $(call baseheader, $(1), tabernacle)

GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

# Main commands
all: build

build:
	cd docker/neo4j_reset && make build

test:
	cd docker/neo4j_reset && make test

docker:
	cd docker/neo4j_reset && make docker

docker-push:
	cd docker/neo4j_reset && make docker-push

deploy-groups:
	ansible-playbook -v -i inventory/gce/ ansible/deploy_groups.yml

# Instances with old base images / outdated playbooks etc.
deploy-feature-branch:
	ansible-playbook -v -i inventory/gce/ ansible/deploy_feature_branch.yml

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

deploy-feature-branch-legacy:
	ansible-playbook -v -i inventory/gce/ ansible/legacy_deploy_feature_branch.yml


.PHONY: all build test docker docker-push
