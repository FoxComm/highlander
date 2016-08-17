configure:
	glide install

build:
	go build -o stock-items-consumer main.go consumer.go sku.go

build-linux:
	GOOS=linux $(MAKE) build

docker-build:
	@make build-test
	docker build -t stock-items-consumer .
	
test:
	GOENV=test go test
