FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates
RUN apt-get clean

RUN mkdir -p /shipments-consumer
WORKDIR /shipments-consumer
COPY shipments-consumer /shipments-consumer

CMD /shipments-consumer/shipments-consumer 2>&1 | tee /log/shipment-consumer.log
