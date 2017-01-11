FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /middlewarehouse
WORKDIR /middlewarehouse
COPY middlewarehouse /middlewarehouse
ADD .env /middlewarehouse

EXPOSE 9292
CMD /middlewarehouse/middlewarehouse 2>&1 | tee /logs/middlewarehouse.log
