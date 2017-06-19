use Mix.Config
config :maru, Geronimo.Api,
  versioning: [
    using: :path
  ],
  http: [ip: {0, 0, 0, 0}, port: System.get_env("PORT")]
