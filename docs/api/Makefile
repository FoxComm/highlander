BUILD_DIR?=./build
PORT?=3000

install:
	npm install

build:
	mkdir -p $(BUILD_DIR)
	./node_modules/.bin/aglio -i ./docs/admin.apib -o $(BUILD_DIR)/admin.html -t slate --theme-full-width -w
	./node_modules/.bin/aglio -i ./docs/public.apib -o $(BUILD_DIR)/public.html -t slate --theme-full-width -w

watch:
	mkdir -p $(BUILD_DIR)
	./node_modules/.bin/aglio -i ./docs/*.apib -o $(BUILD_DIR)/ -t slate --theme-full-width --server -p $(PORT)

clean:
	rm -rf $(BUILD_DIR)

.PHONY: install build watch clean
