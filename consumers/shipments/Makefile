configure:
	glide install

build:
	go build -o shipments-consumer

build-linux:
	GOOS=linux $(MAKE) build

docker-build:
	@make build-linux
	docker build -t shipments-consumer .

test:
	GOENV=test go test
