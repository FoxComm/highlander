include ../makelib
header = $(call baseheader, $(1), api-docs)

BUILD_DIR ?= ./build
PORT ?= 3000

all: clean install build

install:
	yarn --pure-lockfile

build:
	$(call header, Building)
	make install
	mkdir -p $(BUILD_DIR)
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/public.apib -o $(BUILD_DIR)/public.html
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/customer.apib -o $(BUILD_DIR)/storefront.html
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/transactions.apib -o $(BUILD_DIR)/transactions.html
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/merchandising.apib -o $(BUILD_DIR)/merchandising.html
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/inventory.apib -o $(BUILD_DIR)/inventory.html

watch_public:
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/public.apib -o $(BUILD_DIR)/ --server -p $(PORT)

watch_customer:
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/customer.apib -o $(BUILD_DIR)/ --server -p $(PORT)

watch_inventory:
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/inventory.apib -o $(BUILD_DIR)/ --server -p $(PORT)

watch_merchandising:
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/merchandising.apib -o $(BUILD_DIR)/ --server -p $(PORT)

watch_transactions:
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/transactions.apib -o $(BUILD_DIR)/ --server -p $(PORT)

clean:
	rm -rf ./node_modules
	rm -rf $(BUILD_DIR)

test:
	$(call header, Testing)
	true

docker:
	$(call header, Dockerizing)
	true

docker-push:
	$(call header, Registering)
	true

.PHONY: install build watch clean test
