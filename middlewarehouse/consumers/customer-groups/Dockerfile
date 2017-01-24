FROM ubuntu:16.04

RUN apt update
RUN apt install -y ca-certificates

ADD customer-groups-agent /
CMD /customer-groups-agent 2>&1 | tee /logs/customer-groups-agent.log
