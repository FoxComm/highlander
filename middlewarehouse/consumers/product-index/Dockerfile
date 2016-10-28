FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates
RUN apt-get clean

RUN mkdir -p /product-index
WORKDIR /product-index
COPY product-index /product-index

CMD ["/product-index/product-index"] 

