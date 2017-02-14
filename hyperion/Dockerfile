FROM elixir:1.4.0-slim

RUN apt-get update && apt-get install -y git postgresql

RUN mkdir -p /hyperion
WORKDIR /hyperion
COPY . /hyperion

RUN mix local.hex --force
RUN mix local.rebar --force
RUN mix deps.get --only prod
RUN MIX_ENV=prod mix compile

EXPOSE 8880
CMD MIX_ENV=prod elixir --detached -S mix run