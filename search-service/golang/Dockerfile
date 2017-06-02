FROM golang:alpine

RUN apk add --no-cache ca-certificates

RUN mkdir -p /search-service
ADD . /go/src/github.com/FoxComm/highlander/search-service/golang
WORKDIR /go/src/github.com/FoxComm/highlander/search-service/golang
RUN go build -o search-service main.go && \
    cp search-service /search-service && \
    rm -rf /go
WORKDIR /search-service

CMD GIN_MODE=release /search-service/search-service 2>&1 | tee /logs/search-service.log
