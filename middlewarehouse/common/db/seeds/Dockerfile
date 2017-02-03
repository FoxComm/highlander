FROM golang:alpine

RUN mkdir -p /middlewarehouse && \
    touch /middlewarehouse/.env
ADD . /go/src/github.com/FoxComm/highlander/middlewarehouse
WORKDIR /go/src/github.com/FoxComm/highlander/middlewarehouse
RUN go build -o common/db/seeds/seeder common/db/seeds/*.go && \
    cp common/db/seeds/seeder /middlewarehouse && \
    rm -rf /go
WORKDIR /middlewarehouse

CMD ["/middlewarehouse/seeder"]
