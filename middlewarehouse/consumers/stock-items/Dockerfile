FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates
RUN apt-get clean

RUN mkdir -p /stock-items-consumer
WORKDIR /stock-items-consumer
COPY stock-items-consumer /stock-items-consumer

CMD /stock-items-consumer/stock-items-consumer 2>&1 | tee /logs/stock-items-consumer.log
