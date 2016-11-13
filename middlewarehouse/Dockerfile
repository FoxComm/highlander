FROM ubuntu:16.04

RUN apt-get update && apt-get install -y ca-certificates
RUN apt-get clean

RUN mkdir -p /middlewarehouse
WORKDIR /middlewarehouse
COPY middlewarehouse /middlewarehouse
ADD .env /middlewarehouse

EXPOSE 9292
CMD ["/middlewarehouse/middlewarehouse"] 

