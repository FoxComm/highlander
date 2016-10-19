FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates
RUN apt-get clean

RUN mkdir -p /product-search
WORKDIR /product-search
COPY product-search /product-search

EXPOSE 5492
CMD ["/product-search/product-search"] 

