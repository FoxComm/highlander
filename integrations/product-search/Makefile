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
	@echo "--- Building \033[33mproduct-search\033[0m"
	make prep
	cd $(BUILD_PATH) && go build -o product-search main.go update_payload.go

docker:
	@echo "--- Dockerizing \033[33mproduct-search\033[0m"
	true

docker-push:
	@echo "--- Registering \033[33mproduct-search\033[0m"
	true

test:
	@echo "--- Testing \033[33mproduct-search\033[0m"
	true

.PHONY: build test clean docker docker-push
