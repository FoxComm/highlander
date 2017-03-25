FROM golang:alpine

RUN apk add --no-cache ca-certificates

RUN mkdir -p /shipstation-consumer
ADD . /go/src/github.com/FoxComm/highlander/middlewarehouse
WORKDIR /go/src/github.com/FoxComm/highlander/middlewarehouse
RUN go build -o consumers/shipstation/shipstation-consumer consumers/shipstation/*.go && \
    cp consumers/shipstation/shipstation-consumer /shipstation-consumer && \
    rm -rf /go
WORKDIR /shipstation-consumer

CMD /shipstation-consumer/shipstation-consumer 2>&1 | tee /logs/shipstation-consumer.log
