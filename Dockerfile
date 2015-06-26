FROM ubuntu:vivid

RUN apt-get install -y curl

RUN curl -sL https://deb.nodesource.com/setup_iojs_2.x | bash -
RUN apt-get -y update
RUN apt-get install -y iojs make

ADD . /app

WORKDIR /app

RUN make setup

EXPOSE 3000

CMD ["npm", "start"]
