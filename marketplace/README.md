# Marketplace

Welcome to the new Marketplace application, written in Phoenix.

## Getting Started
To get started, please follow the below instructions:
  * Get Erlang: `brew install erlang` or go [the site](http://www.erlang.org)
  * Get Elixir: `brew install elixir` or go [the site](http://www.elixir-lang.org)
  * Install dependencies with `mix deps.get`
  * Create and migrate your database with `source .env && mix ecto.create && mix ecto.migrate`
  * Seed the database with: `source .env && mix run priv/repo/seeds.exs`
  * To start Phoenix with env variables from .env and .env.local run `source .env && mix phoenix.server`

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

## Stripe integration
In order business account to work you should provide stripe private key via STRIPE_API_KEY env variable. You can add it to `.env.local` file which would be
imported on `.env.local` import

```
export STRIPE_API_KEY=sp_key
```
It should be a key from Stripe Managed Account
### Testing Stripe
To create new business account you should use test account and routing numbers for stripe.
https://stripe.com/docs/testing#routing-numbers

## JWT verifying

The application looks for the following environment variable

  * `PUBLIC_KEY` : the full name of the public key

You can set this variable in `.env.local` before running `source .env`.
