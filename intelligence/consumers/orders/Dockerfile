FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates \
    && apt-get clean

RUN mkdir -p /orders-consumer
WORKDIR /orders-consumer
COPY orders-consumer /orders-consumer

CMD ["/orders-consumer/orders-consumer"] 
