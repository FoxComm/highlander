FROM clojure:latest

RUN mkdir -p /build
WORKDIR /build
COPY . /build

RUN mkdir -p /fox-notifications
WORKDIR /fox-notifications
RUN mv /build/target/messaging.jar /fox-notifications
RUN rm  -rf /build

CMD java $JAVA_OPTS -jar messaging.jar
