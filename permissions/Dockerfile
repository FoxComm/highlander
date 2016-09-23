FROM elixir:1.3.2

RUN apt-get update && apt-get install -y build-essential curl git nodejs npm
RUN npm install -g n
RUN npm install -g gulp
RUN n stable

RUN mkdir -p /marketplace
WORKDIR /marketplace
COPY . /marketplace

RUN mix local.hex --force
RUN mix local.rebar --force
RUN mix deps.get

RUN npm install

EXPOSE 4002
CMD ["mix", "phoenix.server"]
