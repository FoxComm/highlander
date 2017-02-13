FROM mhart/alpine-node:6.9.1

RUN npm install -g gulp

RUN mkdir -p /the-perfect-gourmet
WORKDIR /the-perfect-gourmet
COPY . /the-perfect-gourmet

CMD gulp server 2>&1 | tee /logs/storefront.log
