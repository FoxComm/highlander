include ../makelib
header = $(call baseheader, $(1), tabernacle)

GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

# Main commands
all: build

build:
	$(call header, Building)
	true

lint:
	ansible-lint -x ANSIBLE0007,ANSIBLE0004 ansible/*.yml

test:
	$(call header, Testing)
	./terraform/testfmt.sh

debug:
	ansible-playbook -v -i inventory/gce ansible/debug.yml

deploy-stage:
	ansible-playbook -v -i inventory/gce/ ansible/test_stage.yml

deploy-demo:
	ansible-playbook -v -i inventory/gce/ ansible/test_stage.yml

deploy-feature-branch:
	ansible-playbook -v -i inventory/gce/ ansible/provision_feature_branch.yml

docker:
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

.PHONY: all build lint test docker docker-push
