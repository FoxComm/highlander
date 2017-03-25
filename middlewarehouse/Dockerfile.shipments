FROM golang:alpine

RUN apk add --no-cache ca-certificates

RUN mkdir -p /shipments-consumer
ADD . /go/src/github.com/FoxComm/highlander/middlewarehouse
WORKDIR /go/src/github.com/FoxComm/highlander/middlewarehouse
RUN go build -o consumers/shipments/shipments-consumer consumers/shipments/*.go && \
    cp consumers/shipments/shipments-consumer /shipments-consumer/ && \
    rm -rf /go
WORKDIR /shipments-consumer

CMD /shipments-consumer/shipments-consumer 2>&1 | tee /log/shipment-consumer.log
