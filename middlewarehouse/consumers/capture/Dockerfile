FROM golang

RUN curl https://glide.sh/get | sh

ADD . /go/src/github.com/FoxComm/highlander/middlewarehouse
RUN cd /go/src/github.com/FoxComm/highlander/middlewarehouse/consumers/capture && glide install
RUN go install github.com/FoxComm/highlander/middlewarehouse/consumers/capture -o capture-consumer

ENTRYPOINT /go/bin/capture-consumer
