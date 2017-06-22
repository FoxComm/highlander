FROM elixir:1.4.0-slim

RUN apt-get update && apt-get install -y git build-essential

RUN mkdir -p /geronimo
WORKDIR /geronimo
COPY . /geronimo

RUN mix local.hex --force
RUN mix local.rebar --force
RUN mix deps.get --only prod
RUN MIX_ENV=prod mix compile

CMD MIX_ENV=prod elixir -S mix run --no-compile --no-halt
