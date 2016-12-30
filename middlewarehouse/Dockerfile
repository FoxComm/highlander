FROM golang

RUN curl https://glide.sh/get | sh

ADD . /go/src/github.com/FoxComm/highlander/middlewarehouse
RUN cd /go/src/github.com/FoxComm/highlander/middlewarehouse && glide install --debug

RUN go install github.com/FoxComm/highlander/middlewarehouse

EXPOSE 9292
CMD ["/go/bin/middlewarehouse"]
