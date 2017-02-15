FROM ubuntu:16.04

RUN apt-get update && apt-get install -y build-essential curl git nodejs npm && rm -rf /var/lib/apt/lists/*
RUN npm install -g n
RUN n 6.9.1
RUN npm install -g gulp

RUN mkdir -p /ashes
WORKDIR /ashes
COPY . /ashes

EXPOSE 4000
CMD NODE_ENV=production gulp server --optimize_for_size --max_old_space_size=1536 --stack_size=4096 2>&1 | tee /logs/ashes.log
