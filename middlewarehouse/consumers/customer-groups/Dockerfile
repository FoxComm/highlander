FROM golang:alpine

RUN apk add --no-cache ca-certificates

RUN mkdir -p /customer-groups-consumer
ADD . /go/src/github.com/FoxComm/highlander/middlewarehouse
WORKDIR /go/src/github.com/FoxComm/highlander/middlewarehouse
RUN go build -o consumers/customer-groups/customer-groups-consumer consumers/customer-groups/*.go && \
    cp consumers/customer-groups/customer-groups-consumer /customer-groups-consumer && \
    rm -rf /go
WORKDIR /customer-groups-consumer

CMD /customer-groups-consumer/customer-groups-consumer 2>&1 | tee /logs/customer-groups-consumer.log
