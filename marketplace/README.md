# Marketplace

Welcome to the new Marketplace application, written in Phoenix.

## Getting Started
To get started, please follow the below instructions:
  * Get Erlang: `brew install erlang` or go [the site](http://www.erlang.org)
  * Get Elixir: `brew install elixir` or go [the site](http://www.elixir-lang.org)
  * Install dependencies with `mix deps.get`
  * Create and migrate your database with `mix ecto.create && mix ecto.migrate`
  * Seed the database with: `mix run priv/repo/seeds.exs`
  * Start Phoenix endpoint with `mix phoenix.server`

Now you can visit [`localhost:4003`](http://localhost:4003) from your browser.

## System Requirements
  * PostgreSQL: `psql --version`
  * Node.js: `node --version`

## Interactions with Other Systems
This application interacts with both `phoenix` and `solomon`.  The following environment
variables must be set:
  * `PHOENIX_URL`
  * `PHOENIX_PORT`
  * `SOLOMON_URL`
  * `SOLOMON_PORT`

For your convenience, there is already a .env file in the root of this directory that is set with the default of those applications.
You can simply run `source .env` and then `mix phoenix.server`
