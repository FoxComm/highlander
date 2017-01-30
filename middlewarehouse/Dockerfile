FROM golang:alpine

RUN apk add --no-cache ca-certificates

RUN mkdir -p /middlewarehouse
ADD . /go/src/github.com/FoxComm/highlander/middlewarehouse
WORKDIR /go/src/github.com/FoxComm/highlander/middlewarehouse
RUN go build -o middlewarehouse main.go && \
    cp middlewarehouse /middlewarehouse && \
    rm -rf /go
WORKDIR /middlewarehouse
ADD .env /middlewarehouse

EXPOSE 9292
CMD /middlewarehouse/middlewarehouse 2>&1 | tee /logs/middlewarehouse.log
