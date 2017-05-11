FROM mhart/alpine-node:7.7.1

RUN npm install -g gulp

RUN mkdir -p /ashes
WORKDIR /ashes
COPY . /ashes

CMD NODE_ENV=production node ./server/index.js 2>&1 | tee /logs/ashes.log
