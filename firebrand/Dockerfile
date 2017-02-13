FROM mhart/alpine-node:6.9.1

RUN npm install -g gulp

RUN mkdir -p /firebrand
WORKDIR /firebrand
COPY . /firebrand

CMD gulp server 2>&1 | tee /logs/storefront.log
