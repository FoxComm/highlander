FROM golang:alpine

RUN apk add --no-cache ca-certificates

RUN mkdir -p /stock-items-consumer
ADD . /go/src/github.com/FoxComm/highlander/middlewarehouse
WORKDIR /go/src/github.com/FoxComm/highlander/middlewarehouse
RUN go build -o consumers/stock-items/stock-items-consumer consumers/stock-items/*.go && \
    cp consumers/stock-items/stock-items-consumer /stock-items-consumer/ && \
    rm -rf /go
WORKDIR /stock-items-consumer

CMD /stock-items-consumer/stock-items-consumer 2>&1 | tee /logs/stock-items-consumer.log
