FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates \
      && apt-get clean

RUN mkdir -p /suggester
WORKDIR /suggester
COPY suggester /suggester

CMD "/suggester/suggester" 2>&1 | tee /logs/suggester.log

