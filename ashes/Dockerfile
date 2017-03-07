FROM mhart/alpine-node:7.7.1

RUN npm install -g gulp

RUN mkdir -p /ashes
WORKDIR /ashes
COPY . /ashes

CMD NODE_ENV=production gulp server --optimize_for_size --max_old_space_size=1536 --stack_size=4096 2>&1 | tee /logs/ashes.log
