FROM openjdk:8-alpine

RUN mkdir -p /phoenix/seeder-classes
RUN mkdir -p /phoenix/sql

COPY seeder/target/scala-2.11/phoenix-scala-assembly-1.0.jar /phoenix/phoenix.jar
COPY seeder-classes/ /phoenix/seeder-classes/
COPY sql/ /phoenix/sql

WORKDIR /phoenix

EXPOSE 9090
CMD java $JAVA_OPTS -server -cp phoenix.jar server.Main 2>&1 | tee /logs/phoenix.log
