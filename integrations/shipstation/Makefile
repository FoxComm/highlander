BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/integrations/shipstation
HIGHLANDER_PATH=$(CURDIR)/../..

prep:
	rm -rf $(BUILD_ROOT_PATH)
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH)

glide: prep
	cd $(BUILD_PATH) && glide install

build: glide
	cd $(BUILD_PATH) && go build -o shipstation main.go

build-linux: glide
	cd $(BUILD_PATH) && GOOS=linux $(MAKE) build

docker:
	docker build -t shipstation-consumer .

docker-push:
	docker tag shipstation-consumer docker-stage.foxcommerce.com:5000/shipstation-consumer:master
	docker push docker-stage.foxcommerce.com:5000/shipstation-consumer:master

.PHONY: build
