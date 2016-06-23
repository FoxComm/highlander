BUILD_DIR?=./build
PORT?=3000

all: clean install build

install:
	npm install

build:
	mkdir -p $(BUILD_DIR)
	./node_modules/.bin/aglio -i ./docs/admin.apib -o $(BUILD_DIR)/admin.html -t flatly --theme-full-width -w
	./node_modules/.bin/aglio -i ./docs/customer.apib -o $(BUILD_DIR)/customer.html -t flatly --theme-full-width -w
	./node_modules/.bin/aglio -i ./docs/public.apib -o $(BUILD_DIR)/public.html -t flatly --theme-full-width -w
	./node_modules/.bin/aglio -i ./docs/admin_merchandising.apib -o $(BUILD_DIR)/admin-merchandising.html -t flatly --theme-full-width -w

watch_public:
	./node_modules/.bin/aglio -i ./docs/public.apib -o $(BUILD_DIR)/ -t flatly --theme-full-width --server -p $(PORT)

watch_customer:
	./node_modules/.bin/aglio -i ./docs/customer.apib -o $(BUILD_DIR)/ -t flatly --theme-full-width --server -p $(PORT)

watch:
	./node_modules/.bin/aglio -i ./docs/admin.apib -o $(BUILD_DIR)/ -t flatly --theme-full-width --server -p $(PORT)

clean:
	rm -rf $(BUILD_DIR)

.PHONY: install build watch clean
