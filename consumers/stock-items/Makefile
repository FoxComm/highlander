build:
	go build -o stock-items-consumer main.go consumer.go sku.go

build-linux:
	GOOS=linux go build -o stock-items-consumer main.go consumer.go sku.go

docker-build:
	@make build
	docker build -t stock-items-consumer .
	
test:
	GOENV=test go test
