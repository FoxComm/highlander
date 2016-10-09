BUILD_DIR?=./build
PORT?=3000

all: clean install build

install:
	npm install

build:
	mkdir -p $(BUILD_DIR)
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/public.apib -o $(BUILD_DIR)/public.html	
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/customer.apib -o $(BUILD_DIR)/storefront.html
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/transactions.apib -o $(BUILD_DIR)/transactions.html
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/merchandising.apib -o $(BUILD_DIR)/merchandising.html

watch_public:
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/public.apib -o $(BUILD_DIR)/ -t flatly --theme-full-width --server -p $(PORT)

watch_customer:
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/customer.apib -o $(BUILD_DIR)/ -t flatly --theme-full-width --server -p $(PORT)

watch_merchandising:
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/merchandising.apib -o $(BUILD_DIR)/ -t flatly --theme-full-width --server -p $(PORT)

watch_transactions:
	./node_modules/.bin/aglio --theme-template ./template/index.jade -i ./docs/transactions.apib -o $(BUILD_DIR)/ -t flatly --theme-full-width --server -p $(PORT)

clean:
	rm -rf $(BUILD_DIR)

.PHONY: install build watch clean
