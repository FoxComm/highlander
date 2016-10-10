FROM ubuntu:16.04

RUN apt-get update && apt-get install -y build-essential curl git nodejs npm
RUN npm install -g n
RUN npm install -g gulp
RUN n stable


RUN mkdir -p /the-perfect-gourmet
WORKDIR /the-perfect-gourmet
COPY . /the-perfect-gourmet

EXPOSE 4041
CMD ["gulp", "server"]
