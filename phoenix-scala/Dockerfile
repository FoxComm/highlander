FROM openjdk:8

RUN mkdir -p /phoenix/gatling-classes
RUN mkdir -p /phoenix/sql

COPY gatling/target/scala-2.11/phoenix-scala-assembly-1.0.jar /phoenix/phoenix.jar
COPY gatling-classes/ /phoenix/gatling-classes/
COPY sql/ /phoenix/sql

WORKDIR /phoenix

EXPOSE 9090
CMD java $JAVA_OPTS -cp phoenix.jar server.Main
