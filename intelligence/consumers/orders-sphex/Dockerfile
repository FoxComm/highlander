FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates \
    && apt-get clean

RUN mkdir -p /orders-sphex
WORKDIR /orders-sphex
COPY orders-sphex /orders-sphex

CMD ["/orders-sphex/orders-sphex"]