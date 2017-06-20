FROM ubuntu:16.04

RUN apt-get update && apt-get install -y python3 python3-pip \
    && apt-get clean

RUN pip3 install numpy scipy Flask neo4j-driver==1.1.0

RUN mkdir -p /anthill
ADD . /anthill/

CMD ["/anthill/anthill.sh"]
