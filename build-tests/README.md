# build-tests

API based tests automation framework, initially created for running build verification test suite.
The goal of BVT as a test suite is to run sanity checks to make sure build is OK.

#### Installation

Install dependencies:

	npm install

Install Allure CLI.

Ubuntu:

	sudo apt-add-repository ppa:yandex-qatools/allure-framework
	sudo apt-get update
	sudo apt-get install allure-commandline

MacOS:

	brew tap qatools/formulas
	brew install allure-commandline

Export env variable to point at environment against which tests should run:

	export BVT_ENV={env_name}

You can either use one of environments listed in `config.js`, or add there your own by the analogy of existing ones.

A list of available environments:

* STAGE
* STAGE-TPG
* STAGE-TD
* TEST
* FEATURE-BRANCH-APPLE
* APPLIANCE-KANGAROOS

**Make sure to specify the variable value in upper case!**

---

#### Run Tests Manually

To run build verification tests:

	make test-bvt

**[WIP]** To run API tests:

	make test-api

To configure API test run with specific tests you need:


	ava -v --concurrency=3 --match='[api.{area-to-test}]*' > results.txt 2>&1


* `{area-to-test}` should be replaced with something you want test, e.g.:
`--match='[api.cart]*'`
You can have multiple `--match` parameters in your run command.

* `--concurrency=3` is a number of concurrent threads in which test files are executed.
I wouldn't recommend increasing this parameter, yet.

---

#### Reporting

To generate report, run:

	make report

To open report, run:

	allure report open

