dependencies:
	go get github.com/onsi/ginkgo/ginkgo

build:
	go build main.go

test:
	GOENV=test go test $(shell go list ./... | grep -v /vendor/)
