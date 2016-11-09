use Mix.Config

# For production, we configure the host to read the PORT
# from the system environment. Therefore, you will need
# to set PORT=80 before running your server.
#
# You should also configure the url host to something
# meaningful, we use this information when generating URLs.
#
# Finally, we also include the path to a manifest
# containing the digested version of static files. This
# manifest is generated by the mix phoenix.digest task
# which you typically run after static files are built.
config :marketplace, Marketplace.Endpoint,
  http: [port: 4003],
  url: [host: "marketplace.service.consul", port: 4003],
  cache_static_manifest: "priv/static/manifest.json"

# Do not print debug messages in production
config :logger, level: :info

# Configure your database
config :marketplace, Marketplace.Repo,
  adapter: Ecto.Adapters.Postgres,
  username: System.get_env("DB_USER"),
  password: System.get_env("DB_PASSWORD"),
  database: System.get_env("DB_NAME"),
  hostname: System.get_env("DB_HOST"),
  pool_size: 10

config :marketplace, Marketplace.MerchantAccount,
  phoenix_url: System.get_env("PHOENIX_URL"),
  phoenix_port: System.get_env("PHOENIX_PORT"),
  solomon_url: System.get_env("SOLOMON_URL"),
  solomon_port: System.get_env("SOLOMON_PORT"),
  stripe_private_key: System.get_env("STRIPE_PRIVATE_KEY")

# configure jwt auth
config :marketplace, Marketplace.JWTAuth,
  public_key_path: System.get_env("public_keys_dest_dir"),
  public_key: "public_key.pem"

# Finally import the config/prod.secret.exs
# which should be versioned separately.
# import_config "prod.secret.exs"
