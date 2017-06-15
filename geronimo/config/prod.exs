use Mix.Config
config :maru, Geronimo.Api,
  versioning: [
    using: :path
  ],
  http: [port: System.get_env("GERONIMO_PORT")]