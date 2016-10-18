# This file is responsible for configuring your application
# and its dependencies with the aid of the Mix.Config module.
#
# This configuration file is loaded before any dependency and
# is restricted to this project.
use Mix.Config

# General application configuration
config :marketplace,
  ecto_repos: [Marketplace.Repo]

# Configures the endpoint
config :marketplace, Marketplace.Endpoint,
  url: [host: "localhost"],
  secret_key_base: "XFOss4H2UM4YqSAYY/ChSUfSqIzQt5fT7Xw9dmQZbOLQyXGiS1jsQjHGghhey/+d",
  render_errors: [view: Marketplace.ErrorView, accepts: ~w(html json)],
  pubsub: [name: Marketplace.PubSub,
           adapter: Phoenix.PubSub.PG2]

# Configures Elixir's Logger
config :logger, :console,
  format: "$time $metadata[$level] $message\n",
  metadata: [:request_id]

# Import environment specific config. This must remain at the bottom
# of this file so it overrides the configuration defined above.
import_config "#{Mix.env}.exs"
