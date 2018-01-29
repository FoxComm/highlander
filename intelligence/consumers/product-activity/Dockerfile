FROM ubuntu:17.10

RUN apt-get update && apt-get install -y ca-certificates && apt-get clean

RUN mkdir -p /product-activity
WORKDIR /product-activity
COPY product-activity /product-activity

CMD /product-activity/product-activity  2>&1 | tee /logs/product-activity.log
