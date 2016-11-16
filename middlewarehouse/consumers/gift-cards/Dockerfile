FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates
RUN apt-get clean

RUN mkdir -p /gift-card-consumer
WORKDIR /gift-card-consumer
COPY gift-card-consumer /gift-card-consumer

CMD ["/gift-card-consumer/gift-cards"] 
