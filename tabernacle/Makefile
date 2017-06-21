include ../makelib
header = $(call baseheader, $(1), tabernacle)

GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

# Main commands
all: build

build:
	cd docker/neo4j && make build
	cd docker/neo4j_reset && make build

test:
	true

docker:
	cd docker/neo4j && make docker
	cd docker/neo4j_reset && make docker

docker-push:
	cd docker/neo4j && make docker-push
	cd docker/neo4j_reset && make docker-push

deploy-stage:
	ansible-playbook -v -i inventory/static/dev ansible/deploy_stage.yml

deploy-test:
	ansible-playbook -v -i inventory/static/dev ansible/deploy_test.yml

# Instances with old base images / outdated playbooks etc.
deploy-storefront:
	ansible-playbook -v -i inventory/static/dev ansible/legacy_deploy_storefront.yml

deploy-td-stage:
	ansible-playbook -v -i inventory/static/topdrawer ansible/legacy_deploy_storefront.yml

deploy-tpg-stage:
	ansible-playbook -v -i inventory/static/tpg ansible/legacy_deploy_storefront.yml

deploy-feature-branch-legacy:
	ansible-playbook -v -i inventory/gce/ ansible/legacy_deploy_feature_branch.yml


.PHONY: all build test docker docker-push
