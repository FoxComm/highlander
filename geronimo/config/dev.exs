use Mix.Config
config :maru, Geronimo.Api,
  versioning: [
    using: :path
  ],
  http: [port: 8880]


