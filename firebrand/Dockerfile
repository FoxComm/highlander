FROM mhart/alpine-node:6.9.1

RUN npm install -g gulp

RUN mkdir -p /firebrand
WORKDIR /firebrand
COPY . /firebrand

CMD NODE_ENV=production gulp server --optimize_for_size --max_old_space_size=2048 --stack_size=4096 2>&1 | tee /logs/storefront.log
