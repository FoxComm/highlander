GOPATH = /tmp/go
BUILD_ROOT_PATH=$(GOPATH)/src/github.com/FoxComm/highlander
BUILD_PATH=$(BUILD_ROOT_PATH)/consumers/product-index
HIGHLANDER_PATH=$(CURDIR)/../..

DOCKER_REPO ?= docker-stage.foxcommerce.com:5000
DOCKER_TAG ?= product-index-consumer
DOCKER_BRANCH ?= master

prep:
	mkdir -p $(GOPATH)/src/github.com/FoxComm
	ln -s $(HIGHLANDER_PATH) $(BUILD_ROOT_PATH) || true

glide: prep
	cd $(BUILD_PATH) && glide install

build: glide
	cd $(BUILD_PATH) && go build -o product-index-consumer *.go

docker:
	docker build -t $(DOCKER_TAG) .

docker-push:
	docker tag $(DOCKER_TAG) $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)
	docker push $(DOCKER_REPO)/$(DOCKER_TAG):$(DOCKER_BRANCH)

clean:
	rm -rf ./vendor

test: glide
	cd $(BUILD_PATH) && GOENV=test go test `glide nv`
