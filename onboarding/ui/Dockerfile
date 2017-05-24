FROM ubuntu:16.04

RUN apt-get update && apt-get install -y build-essential curl git nodejs npm
RUN apt-get clean
RUN npm install -g n
RUN npm install -g gulp
RUN n stable

RUN mkdir -p /onboarding-ui
WORKDIR /onboarding-ui
COPY . /onboarding-ui

RUN npm install
RUN gulp build

EXPOSE 4042
CMD ["gulp", "dev"]
