FROM elixir:1.3.2

RUN mkdir -p /marketplace
WORKDIR /marketplace
COPY . /marketplace

RUN mix local.hex --force
RUN mix deps.get

EXPOSE 4000
CMD ["mix", "phoenix.server"]
