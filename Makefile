build:
	go build main.go

test:
	GOENV=test go test $(shell go list ./... | grep -v /vendor/)
