use Mix.Config

config :solomon, Solomon.Endpoint,
  secret_key_base: System.get_env("SOLOMON_SECRET")

config :solomon, Solomon.Repo,
  adapter: Ecto.Adapters.Postgres,
  username: System.get_env("DB_USER"),
  password: System.get_env("DB_PASSWORD"),
  database: System.get_env("DB_NAME"),
  hostname: System.get_env("DB_HOST"),
  pool_size: 10,
  extensions: [
    {Solomon.LTree, :copy}
  ]
