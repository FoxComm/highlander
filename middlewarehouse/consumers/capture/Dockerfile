FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates
RUN apt-get clean

RUN mkdir -p /capture-consumer
WORKDIR /capture-consumer
COPY capture-consumer /capture-consumer

CMD /capture-consumer/capture-consumer 2>&1 | tee /logs/capture-consumer.log
