FROM openjdk:8-alpine

RUN mkdir -p /phoenix/sql

COPY phoenix/target/scala-2.11/phoenix-assembly-1.0.jar /phoenix/phoenix.jar

COPY sql/ /phoenix/sql

COPY seeder/target/scala-2.11/seeder-assembly-1.0.jar /phoenix/phoenix-seeder.jar

WORKDIR /phoenix
