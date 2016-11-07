FROM ubuntu:16.04

RUN mkdir -p /middlewarehouse
WORKDIR /middlewarehouse
COPY seeder /middlewarehouse
RUN touch /middlewarehouse/.env

CMD ["/middlewarehouse/seeder"]
