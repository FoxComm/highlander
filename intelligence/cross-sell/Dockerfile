FROM ubuntu:16.04

RUN apt-get update && apt-get install -y python3 python3-pip \
    && apt-get clean

RUN pip3 install numpy scipy Flask neomodel

RUN mkdir -p /cross-sell
ADD . /cross-sell/

CMD ["/cross-sell/cross-sell.sh"]
