FROM ubuntu:16.04

RUN apt update
RUN apt install -y ca-certificates

ADD shipstation /
ADD .env /
CMD ["/shipstation"]
