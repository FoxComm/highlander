FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates
RUN apt-get clean

RUN mkdir -p /river-rock
WORKDIR /river-rock
COPY river-rock /river-rock

CMD ["/river-rock/river-rock"] 

