FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates \
    && apt-get clean

RUN mkdir -p /eggcrate
WORKDIR /eggcrate
COPY eggcrate /eggcrate

EXPOSE 10020
CMD ["/eggcrate/eggcrate"] 
