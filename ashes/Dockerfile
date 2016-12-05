FROM ubuntu:16.04

RUN apt-get update && apt-get install -y build-essential curl git nodejs npm
RUN npm install -g n
RUN n 6.9.1
RUN npm install -g gulp

RUN mkdir -p /ashes
WORKDIR /ashes
COPY . /ashes

EXPOSE 4000
CMD ["gulp", "server"]
