FROM ubuntu:16.04

RUN apt update
RUN apt install -y ca-certificates

ADD shipstation-consumer /
CMD /shipstation-consumer 2>&1 | tee /logs/shipstation-consumer.log
