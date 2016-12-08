FROM ubuntu:16.04

RUN apt-get update && apt-get install -y build-essential curl git nodejs npm
RUN npm install -g n
RUN npm install -g gulp
RUN n stable

RUN mkdir -p /search
WORKDIR /search
COPY . /search

EXPOSE 3000
CMD ["npm", "start"]
