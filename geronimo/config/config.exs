# This file is responsible for configuring your application
# and its dependencies with the aid of the Mix.Config module.
use Mix.Config

config :geronimo, ecto_repos: [Geronimo.Repo]
config :geronimo, Geronimo.Repo,
  adapter: Ecto.Adapters.Postgres,
  username: System.get_env("GERONIMO_DB_USER"),
  password: System.get_env("GERONIMO_DB_PASSWORD"),
  database: System.get_env("GERONIMO_DB_NAME"),
  hostname: System.get_env("GERONIMO_DB_HOST"),
  pool_size: 15,
  types: Geronimo.PostgresTypes

config :geronimo,
  public_key: System.get_env("PUBLIC_KEY")

config :geronimo,
  kafka_host: System.get_env("BROKER_HOST"),
  kafka_port: System.get_env("BROKER_PORT"),
  consumer_group: System.get_env("CONSUMER_GROUP"),
  start_kafka_worker: System.get_env("START_WORKER"),
  schema_registry_url: System.get_env("SCHEMA_REGISTRY_URL"),
  schema_registry_port: System.get_env("SCHEMA_REGISTRY_PORT")

config :kafka_ex,
  disable_default_worker: true,
  auto_commit: false,
  use_ssl: false

# This configuration is loaded before any dependency and is restricted
# to this project. If another project depends on this project, this
# file won't be loaded nor affect the parent project. For this reason,
# if you want to provide default values for your application for
# 3rd-party users, it should be done in your "mix.exs" file.

# You can configure for your application as:
#
#     config :geronimo, key: :value
#
# And access this configuration in your application as:
#
#     Application.get_env(:geronimo, :key)
#
# Or configure a 3rd-party app:
#
#     config :logger, level: :info
#

# It is also possible to import configuration files, relative to this
# directory. For example, you can emulate configuration per environment
# by uncommenting the line below and defining dev.exs, test.exs and such.
# Configuration from the imported file will override the ones defined
# here (which is why it is important to import them last).
#

import_config "#{Mix.env}.exs"
