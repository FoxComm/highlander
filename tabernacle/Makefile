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
	./terraform/testfmt.sh

fmt:
	terraform fmt terraform/

deploy-stage:
	ansible-playbook -v -i inventory/gce/ ansible/test_stage.yml

deploy-demo:
	ansible-playbook -v -i inventory/gce/ ansible/demo.yml --extra-vars='{ "first_run": true }'

update-demo:
	ansible-playbook -v -i inventory/gce/ ansible/demo.yml --extra-vars='{ "with_data_import": false }'

deploy-feature-branch:
	ansible-playbook -v -i inventory/gce/ ansible/provision_feature_branch.yml

docker:
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

clean:
	true

.PHONY: all build clean test docker docker-push
