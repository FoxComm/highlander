# This file is responsible for configuring your application
# and its dependencies with the aid of the Mix.Config module.
use Mix.Config
config :maru, Hyperion.API,
  versioning: [
    using: :path
  ],
  http: [port: 8880]

config :hyperion, ecto_repos: [Hyperion.Repo]

# Configure your database
config :hyperion, Hyperion.Repo,
  adapter: Ecto.Adapters.Postgres,
  username: System.get_env("HYPERION_DB_USER"),
  password: System.get_env("HYPERION_DB_PASSWORD"),
  database: System.get_env("HYPERION_DB_NAME"),
  hostname: System.get_env("HYPERION_DB_HOST"),
  pool_size: 10

config :ex_aws,
  access_key_id: [{:system, "AWS_ACCESS_KEY_ID"}, :instance_role],
  secret_access_key: [{:system, "AWS_SECRET_ACCESS_KEY"}, :instance_role],
  region: "us-west-2"

config :hyperion,
  mws_secret_access_key: System.get_env("MWS_SECRET_ACCESS_KEY"),
  mws_access_key_id: System.get_env("MWS_ACCESS_KEY_ID")

config :hyperion,
  phoenix_url:

config :tirexs,
  elastic_uri: System.get_env("ELASTIC_URL")
# This configuration is loaded before any dependency and is restricted
# to this project. If another project depends on this project, this
# file won't be loaded nor affect the parent project. For this reason,
# if you want to provide default values for your application for
# 3rd-party users, it should be done in your "mix.exs" file.

# You can configure for your application as:
#
#     config :context, key: :value
#
# And access this configuration in your application as:
#
#     Application.get_env(:context, :key)
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
#     import_config "#{Mix.env}.exs"
