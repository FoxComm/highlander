GO ?= go
GOPATH := $(CURDIR)/_vendor:$(GOPATH)

# Main commands
all: build

build:
	$(GO) build -o bin/inventory inventory/inventory.go

clean:
	true

lint:
	ansible-lint -x ANSIBLE0007,ANSIBLE0004 ansible/*.yml

test:
	go get github.com/stretchr/testify
	$(GO) test -v ./inventory/

deploy-stage:
	ansible-playbook -v -i bin/envs/staging ansible/stage.yml

deploy-stage-highlander:
	ansible-playbook -v -i bin/envs/staging ansible/highlander_staging.yml

deploy-td-test:
	ansible-playbook -v -i bin/envs/staging ansible/td_test.yml

deploy-tpg-test:
	ansible-playbook -v -i bin/envs/staging ansible/tpg_test.yml

deploy-gatling:
	ansible-playbook -v -i bin/envs/staging ansible/gatling.yml

deploy-gatling-highlander:
	ansible-playbook -v -i bin/envs/staging ansible/highlander_gatling.yml --private-key=$(PRIVATE_KEY)

run-gatling: deploy-gatling
	ansible-playbook -v -i bin/envs/staging ansible/run_gatling.yml

run-gatling-highlander: deploy-gatling-highlander
	ansible-playbook -v -i bin/envs/staging ansible/run_gatling_highlander.yml

docker:
	true

docker-push:
	true


# aws target stage

get-modules:
	terraform get terraform/base/aws_target_stage

plan: get-modules
	terraform plan -state=terraform/envs/aws_target_stage/terraform.tfstate -var-file=terraform/envs/aws_target_stage/env.tfvars terraform/base/aws_target_stage

plan-destroy: get-modules
	terraform plan -destroy -state=terraform/envs/aws_target_stage/terraform.tfstate -var-file=terraform/envs/aws_target_stage/env.tfvars terraform/base/aws_target_stage

apply: get-modules
	terraform apply -state=terraform/envs/aws_target_stage/terraform.tfstate -var-file=terraform/envs/aws_target_stage/env.tfvars terraform/base/aws_target_stage

destroy: get-modules
	terraform destroy -state=terraform/envs/aws_target_stage/terraform.tfstate -var-file=terraform/envs/aws_target_stage/env.tfvars terraform/base/aws_target_stage

.PHONY: all build lint test
