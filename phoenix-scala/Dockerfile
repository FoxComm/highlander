FROM openjdk:8-alpine

RUN mkdir -p /phoenix/sql

COPY phoenix/target/scala-2.11/phoenix-assembly-1.0.jar /phoenix/phoenix.jar

COPY sql/ /phoenix/sql

WORKDIR /phoenix

EXPOSE 9090
CMD java $JAVA_OPTS -server -cp phoenix.jar phoenix.server.Main 2>&1 | tee /logs/phoenix.log
