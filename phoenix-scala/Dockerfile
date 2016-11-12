FROM openjdk:8

RUN mkdir -p /phoenix

COPY gatling/target/scala-2.11/phoenix-scala-assembly-1.0.jar /phoenix/phoenix.jar

WORKDIR /phoenix

EXPOSE 9090
CMD java $JAVA_OPTS -cp phoenix.jar server.Main
