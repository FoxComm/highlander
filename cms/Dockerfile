FROM golang:alpine

RUN apk add --no-cache ca-certificates

RUN mkdir -p /cms
ADD . /go/src/github.com/FoxComm/highlander/cms
WORKDIR /go/src/github.com/FoxComm/highlander/cms
RUN go build -o cms main.go && \
    cp cms /cms && \
    rm -rf /go

WORKDIR /cms
EXPOSE 9293
CMD /cms/cms 2>&1 | tee /logs/cms.log
