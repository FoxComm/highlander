FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates \
    && apt-get clean

RUN mkdir -p /orders-reviews
WORKDIR /orders-reviews
COPY orders-reviews /orders-reviews

CMD /orders-reviews/orders-reviews 2>&1 | tee /logs/orders-reviews.log
