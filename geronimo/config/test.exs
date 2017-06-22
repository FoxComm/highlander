use Mix.Config
config :geronimo, Geronimo.Repo,
  adapter: Ecto.Adapters.Postgres,
  username: System.get_env("GERONIMO_DB_USER"),
  password: System.get_env("GERONIMO_DB_PASSWORD"),
  database: System.get_env("GERONIMO_DB_NAME"),
  hostname: System.get_env("GERONIMO_DB_HOST"),
  pool: Ecto.Adapters.SQL.Sandbox,
  types: Geronimo.PostgresTypes