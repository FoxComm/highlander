use Mix.Config

config :hyperion, Hyperion.Repo,
  adapter: Ecto.Adapters.Postgres,
  username: System.get_env("HYPERION_DB_USER"),
  password: System.get_env("HYPERION_DB_PASSWORD"),
  database: System.get_env("HYPERION_DB_NAME"),
  hostname: System.get_env("HYPERION_DB_HOST"),
  pool: Ecto.Adapters.SQL.Sandbox

config :maru, Hyperion.API,
  versioning: [
    using: :path
  ],
  http: [port: 8880]
