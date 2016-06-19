dependencies:
	go get github.com/onsi/ginkgo/ginkgo

build:
	go build main.go

test: export GOENV=test
test:
	ginkgo -succinct=false -keepGoing=true -r
