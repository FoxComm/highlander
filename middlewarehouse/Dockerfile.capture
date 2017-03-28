FROM golang:alpine

RUN apk add --no-cache ca-certificates

RUN mkdir -p /capture-consumer
ADD . /go/src/github.com/FoxComm/highlander/middlewarehouse
WORKDIR /go/src/github.com/FoxComm/highlander/middlewarehouse
RUN go build -o consumers/capture/capture-consumer consumers/capture/*.go && \
    cp consumers/capture/capture-consumer /capture-consumer && \
    rm -rf /go
WORKDIR /capture-consumer

CMD /capture-consumer/capture-consumer 2>&1 | tee /logs/capture-consumer.log
