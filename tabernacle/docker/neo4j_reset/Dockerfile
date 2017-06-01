FROM tutum/curl:latest

ADD . /usr/local/bin

RUN ["chmod", "+x", "/usr/local/bin/neo4j_reset_password.sh"]

CMD /usr/local/bin/neo4j_reset_password.sh
