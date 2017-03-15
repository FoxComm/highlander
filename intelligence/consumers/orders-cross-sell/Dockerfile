FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates \
    && apt-get clean

RUN mkdir -p /orders-cross-sell
WORKDIR /orders-cross-sell
COPY orders-cross-sell /orders-cross-sell

CMD ["/orders-cross-sell/orders-cross-sell"]
