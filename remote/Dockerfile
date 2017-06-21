FROM golang:alpine

RUN apk add --no-cache ca-certificates

RUN mkdir -p /remote
ADD . /go/src/github.com/FoxComm/highlander/remote
WORKDIR /go/src/github.com/FoxComm/highlander/remote
RUN go build -o remote main.go && \
    cp remote /remote && \
    rm -rf /go
WORKDIR /remote

CMD /remote/remote 2>&1 | tee /logs/remote.log
