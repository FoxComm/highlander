use Mix.Config

config :solomon, Solomon.Endpoint,
  secret_key_base: "C0VuCzlCDYJgbgx9IEZGERHCyRfB3Mf7L4Du+aNQk/Ixrf6vlNr9QNXp8EC2NMle"

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
