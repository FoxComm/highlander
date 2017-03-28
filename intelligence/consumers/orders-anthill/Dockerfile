FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates \
    && apt-get clean

RUN mkdir -p /orders-anthill
WORKDIR /orders-anthill
COPY orders-anthill /orders-anthill

CMD /orders-anthill/orders-anthill 2>&1 | tee /logs/orders-anthill.log
