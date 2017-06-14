FROM openjdk:8-alpine

RUN mkdir -p /phoenix/sql
RUN mkdir -p /phoenix/newrelic

COPY phoenix/target/scala-2.11/phoenix-assembly-1.0.jar /phoenix/phoenix.jar

COPY sql/ /phoenix/sql

COPY newrelic/ /phoenix/newrelic

WORKDIR /phoenix

EXPOSE 9090
CMD java $JAVA_OPTS -javaagent:/phoenix/newrelic/newrelic.jar -server -cp phoenix.jar phoenix.server.Main 2>&1 | tee /logs/phoenix.log