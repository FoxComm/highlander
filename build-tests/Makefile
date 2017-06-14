build:
	npm install

test:
	./node_modules/.bin/ava --verbose --concurrency=3 

report:
	rm -rf allure-report allure-results
	node make-allure-report.js results.txt
	allure generate allure-results
