FROM openjdk:8

RUN mkdir -p /green-river

COPY target/scala-2.11/green-river-assembly-1.0.jar /green-river

WORKDIR /green-river
CMD java $JAVA_OPTS -jar green-river-assembly-1.0.jar
