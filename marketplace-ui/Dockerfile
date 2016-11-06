FROM ubuntu:16.04

RUN apt-get update && apt-get install -y build-essential curl git nodejs npm
RUN apt-get clean
RUN npm install -g n
RUN npm install -g gulp
RUN n stable

RUN mkdir -p /marketplace-ui
WORKDIR /marketplace-ui
COPY . /marketplace-ui

RUN npm install
RUN gulp build

EXPOSE 4042
CMD ["gulp", "dev"]
