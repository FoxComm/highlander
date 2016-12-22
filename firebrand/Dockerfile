FROM ubuntu:16.04

RUN apt-get update && apt-get install -y build-essential curl git nodejs npm
RUN npm install -g n
RUN n 6.9.1
RUN npm install -g gulp

RUN mkdir -p /firebrand
WORKDIR /firebrand
COPY . /firebrand

EXPOSE 4041
CMD gulp server 2>&1 | tee /logs/storefront.log
