build:
	go build -o consumers-skus main.go consumer.go sku.go

build-linux:
	GOOS=linux go build -o consumers-skus main.go consumer.go sku.go

docker-build:
	@make build
	docker build -t stock-items-consumer .
	
test:
	GOENV=test go test
