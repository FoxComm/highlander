defmodule Hyperion.Mixfile do
  use Mix.Project

  def project do
    [app: :hyperion,
     version: "0.1.0",
     elixir: "~> 1.4",
     build_embedded: Mix.env == :prod,
     start_permanent: Mix.env == :prod,
     preferred_cli_env: [espec: :test],
     elixirc_paths: elixirc_paths(Mix.env),
     deps: deps()]
  end

  # Configuration for the OTP application
  #
  # Type "mix help compile.app" for more information
  def application do
    # Specify extra applications you'll use from Erlang/Elixir
    [ extra_applications: (Mix.env == :dev && [:exsync] || []) ++ [:logger, :maru, :postgrex,
                                                                   :ecto, :httpoison, :ex_aws],
     mod: {Hyperion, []}]
  end

  defp elixirc_paths(:test), do: ["lib", "spec/support"]
  defp elixirc_paths(_), do: ["lib"]

  # Dependencies can be Hex packages:
  #
  #   {:my_dep, "~> 0.3.0"}
  #
  # Or git/path repositories:
  #
  #   {:my_dep, git: "https://github.com/elixir-lang/my_dep.git", tag: "0.1.0"}
  #
  # Type "mix help deps" for more examples and options
  defp deps do
    [{:maru, "~> 0.11.2"},
     {:ecto, "~> 2.1"},
     {:postgrex, "~> 0.13.0"},
     {:cors_plug, "~> 1.1"},
     {:ex_aws, "~> 1.0"},
     {:csv, "~> 1.4.2"},
     {:mws_client, github: "FoxComm/elixir-amazon-mws-client"},
     {:exsync, "~> 0.1", only: :dev},
     {:espec, "~> 1.3.2", only: :test},
     {:ex_machina, "~> 2.0", only: :test}]
  end
end
