include ../../makelib
header = $(call baseheader, $(1), product-search)

BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm
HIGHLANDER_ROOT_PATH=$(BUILD_ROOT_PATH)/highlander
BUILD_PATH=$(HIGHLANDER_ROOT_PATH)/integrations/product-search

prep:
	rm -rf $(HIGHLANDER_ROOT_PATH)
	mkdir -p $(BUILD_ROOT_PATH)
	ln -s ${CURDIR}/../.. $(HIGHLANDER_ROOT_PATH)

clean:
	true

build:
	$(call header, Building)
	make prep
	cd $(BUILD_PATH) && go build -o product-search main.go update_payload.go

docker:
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

test:
	$(call header, Testing)
	true

.PHONY: build test clean docker docker-push
