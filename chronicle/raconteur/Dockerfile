FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates \
    && apt-get clean

RUN mkdir -p /raconteur
WORKDIR /raconteur
COPY raconteur /raconteur

CMD ["/raconteur/raconteur"]
