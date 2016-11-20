use Mix.Config

# For development, we disable any cache and enable
# debugging and code reloading.
#
# The watchers configuration can be used to run external
# watchers to your application. For example, we use it
# with brunch.io to recompile .js and .css sources.
config :solomon, Solomon.Endpoint,
  http: [port: {:system, "{PORT"}],
  debug_errors: true,
  code_reloader: true,
  check_origin: false


# Do not include metadata nor timestamps in development logs
config :logger, :console, format: "[$level] $message\n"

# Set a higher stacktrace during development. Avoid configuring such
# in production as building large stacktraces may be expensive.
config :phoenix, :stacktrace_depth, 20

# Configure your database
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

# configure jwt auth
config :solomon, Solomon.JWTAuth,
  private_key: System.get_env("PRIVATE_KEY"),
  public_key: System.get_env("PUBLIC_KEY")

# configure jwt claims
config :solomon, Solomon.JWTClaims,
  tokenTTL: System.get_env("TOKEN_TTL")
