FROM golang:alpine

RUN apk add --no-cache ca-certificates

RUN mkdir -p /gift-card-consumer
ADD . /go/src/github.com/FoxComm/highlander/middlewarehouse
WORKDIR /go/src/github.com/FoxComm/highlander/middlewarehouse
RUN go build -o consumers/gift-cards/gift-card-consumer consumers/gift-cards/*.go && \
    cp consumers/gift-cards/gift-card-consumer /gift-card-consumer && \
    rm -rf /go
WORKDIR /gift-card-consumer

CMD /gift-card-consumer/gift-card-consumer 2>&1 | tee /logs/gift-card-consumer.log
