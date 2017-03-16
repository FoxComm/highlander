defmodule Hyperion.Mixfile do
  use Mix.Project

  def project do
    [app: :hyperion,
     version: "0.1.0",
     elixir: "~> 1.4",
     build_embedded: Mix.env == :prod,
     start_permanent: Mix.env == :prod,
     deps: deps()]
  end

  # Configuration for the OTP application
  #
  # Type "mix help compile.app" for more information
  def application do
    # Specify extra applications you'll use from Erlang/Elixir
    [ extra_applications: (Mix.env == :dev && [:exsync] || []) ++ [:ex_aws, :logger, :maru, :postgrex, :ecto, :httpoison, :tirexs],
     mod: {Hyperion, []}]
  end

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
     {:tirexs, "~> 0.8"},
     {:exsync, "~> 0.1", only: :dev}]
  end
end
