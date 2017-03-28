test:
	./node_modules/.bin/ava --verbose --no-color > results.txt 2>&1

report:
	rm -rf allure-report allure-results
	node make-allure-report.js results.txt
	allure generate allure-results
