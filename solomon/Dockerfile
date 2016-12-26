FROM elixir:1.3.4-slim

RUN apt-get update && apt-get install -y build-essential curl git nodejs npm && rm -rf /var/lib/apt/lists/*
RUN npm install -g n
RUN n 6.9.1
RUN npm install -g gulp

RUN mkdir -p /solomon
WORKDIR /solomon
COPY . /solomon

RUN mix local.hex --force
RUN mix local.rebar --force
RUN mix deps.get
RUN MIX_ENV=prod mix phoenix.gen.secret

RUN npm install
RUN MIX_ENV=prod mix phoenix.digest

EXPOSE 4002
CMD MIX_ENV=prod mix phoenix.server
