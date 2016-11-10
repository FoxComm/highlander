FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates
RUN apt-get clean

RUN mkdir -p /product-index-consumer
WORKDIR /product-index-consumer
COPY product-index-consumer /product-index-consumer

CMD ["/product-index-consumer/product-index-consumer"] 

