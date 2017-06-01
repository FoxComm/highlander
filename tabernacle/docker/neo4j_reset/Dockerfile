FROM gliderlabs/alpine

RUN sudo apt-get update
RUN sudo apt-get -y install curl

ADD . /usr/local/bin

CMD /usr/local/bin/neo4j_reset_password.sh
