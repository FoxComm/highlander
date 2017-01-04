configure-server:
	mkdir -p ./bin
	cd ./bin && wget https://github.com/mozilla/geckodriver/releases/download/v0.12.0/geckodriver-v0.12.0-macos.tar.gz
	cd ./bin && tar -xf geckodriver-v0.12.0-macos.tar.gz
	cd ./bin && wget http://selenium-release.storage.googleapis.com/3.0/selenium-server-standalone-3.0.1.jar
	rm -f ./bin geckodriver-v0.12.0-macos.tar.gz

run-server:
	cd ./bin && java -jar -Dwebdriver.geckodriver.driver=./geckodriver selenium-server-standalone-3.0.1.jar
