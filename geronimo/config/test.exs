use Mix.Config
config :geronimo, Geronimo.Repo,
  adapter: Ecto.Adapters.Postgres,
  username: System.get_env("GERONIMO_DB_USER"),
  password: System.get_env("GERONIMO_DB_PASSWORD"),
  database: System.get_env("GERONIMO_DB_NAME"),
  hostname: System.get_env("GERONIMO_DB_HOST"),
  pool: Ecto.Adapters.SQL.Sandbox,
  types: Geronimo.PostgresTypes

config :exvcr, [
  vcr_cassette_library_dir: "spec/fixture/vcr_cassettes",
  filter_sensitive_data: [
    [pattern: "<PASSWORD>.+</PASSWORD>", placeholder: "PASSWORD_PLACEHOLDER"]
  ],
  filter_url_params: false,
  response_headers_blacklist: []
]