use Mix.Config

# For development, we disable any cache and enable
# debugging and code reloading.
#
# The watchers configuration can be used to run external
# watchers to your application. For example, we use it
# with brunch.io to recompile .js and .css sources.
config :onboarding_service, OnboardingService.Endpoint,
  http: [port: {:system, "PORT"}],
  debug_errors: true,
  code_reloader: true,
  check_origin: false


# Watch static and templates for browser reloading.
config :onboarding_service, OnboardingService.Endpoint,
  live_reload: [
    patterns: [
      ~r{priv/static/.*(js|css|png|jpeg|jpg|gif|svg)$},
      ~r{priv/gettext/.*(po)$},
      ~r{web/views/.*(ex)$},
      ~r{web/templates/.*(eex)$}
    ]
  ]

# Do not include metadata nor timestamps in development logs
config :logger, :console, format: "[$level] $message\n"

# Set a higher stacktrace during development. Avoid configuring such
# in production as building large stacktraces may be expensive.
config :phoenix, :stacktrace_depth, 20

# Configure your database
config :onboarding_service, OnboardingService.Repo,
  adapter: Ecto.Adapters.Postgres,
  username: System.get_env("DB_USER"),
  password: System.get_env("DB_PASSWORD"),
  database: System.get_env("DB_NAME"),
  hostname: System.get_env("DB_HOST"),
  pool_size: 10

config :onboarding_service, OnboardingService.MerchantAccount,
  solomon_service: System.get_env("SOLOMON_SERVICE"),
  solomon_host: System.get_env("SOLOMON_HOST"),
  solomon_jwt: System.get_env("SOLOMON_JWT"),
  stripe_private_key: System.get_env("STRIPE_API_KEY")

# configure jwt auth
config :onboarding_service, OnboardingService.JWTAuth,
  public_key: System.get_env("PUBLIC_KEY")
