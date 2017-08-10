build:
	npm install

test-bvt:
	rm -rf allure-report allure-results
	./node_modules/mocha/bin/mocha --compilers js:babel-core/register 'test/*' --reporter mocha-allure-reporter

report:
	allure generate allure-results
