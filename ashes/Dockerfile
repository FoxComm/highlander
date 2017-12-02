FROM mhart/alpine-node:8.1.0

ARG NPM_TOKEN
RUN mkdir -p /ashes
WORKDIR /ashes

RUN echo "//registry.npmjs.org/:_authToken=248076da-33ef-4d36-9927-bc8e383f6de1" > .npmrc
RUN echo "//registry.npmjs.org/:_authToken=248076da-33ef-4d36-9927-bc8e383f6de1" > /ashes/.npmrc
COPY ./package.json /ashes/package.json
COPY ./yarn.lock /ashes/yarn.lock
COPY ./src/opt /ashes/src/opt

RUN yarn --production --ignore-scripts --ignore-engines && yarn cache clean

# All other src must be copyed AFTER `yarn`, otherwise it breaks caching
COPY ./config /ashes/config
COPY ./server /ashes/server
COPY ./build /ashes/build

# Remove the auth token, for security reasons
RUN rm .npmrc

CMD NODE_ENV=production node ./server/index.js --optimize_for_size --max_old_space_size=1536 --stack_size=4096 2>&1 | tee /logs/ashes.log
