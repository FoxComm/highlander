build:
	npm install

test-bvt:
	./node_modules/.bin/ava -v --concurrency=3 --match='[bvt]*' > results.txt 2>&1

report:
	rm -rf allure-report allure-results
	node make-allure-report.js results.txt
	allure generate allure-results
