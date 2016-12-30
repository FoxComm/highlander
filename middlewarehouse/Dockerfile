FROM golang

RUN sudo add-apt-repository ppa:masterminds/glide
RUN sudo apt-get update
RUN sudo apt-get install glide

ADD . /go/src/github.com/FoxComm/highlander/middlewarehouse
RUN cd /go/src/github.com/FoxComm/highlander/middlewarehouse && glide install

RUN go install github.com/FoxComm/highlander/middlewarehouse

EXPOSE 9292
CMD ["/go/bin/middlewarehouse"]
