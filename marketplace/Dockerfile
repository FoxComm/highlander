FROM elixir:1.3.4-slim

RUN apt-get update && apt-get install -y build-essential curl git nodejs npm
RUN apt-get clean
RUN npm install -g n
RUN npm install -g gulp
RUN n stable

RUN mkdir -p /marketplace
WORKDIR /marketplace
COPY . /marketplace

RUN mix local.hex --force
RUN mix local.rebar --force
RUN mix deps.get

RUN MIX_ENV=prod mix compile
RUN MIX_ENV=prod mix compile.protocols
RUN MIX_ENV=prod mix phoenix.digest

RUN echo "mix ecto.migrate" > ./run_marketplace.sh
RUN echo "MIX_ENV=prod elixir -pa _build/prod/consolidated -S mix phoenix.server" >> ./run_marketplace.sh

RUN chmod +x ./run_marketplace.sh

RUN npm install

EXPOSE 4003
CMD ./run_marketplace.sh
