defmodule Geronimo.Mixfile do
  use Mix.Project

  def project do
    [app: :geronimo,
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
    [extra_applications: (Mix.env == :dev && [:exsync] || []) ++ [:logger, :maru, :timex, :ecto, :postgrex,
                                                                  :timex_ecto, :kafka_ex, :avrolixr, :erlavro,
                                                                  :httpoison],
     mod: {Geronimo, []}]

  end

  defp elixirc_paths(:test), do: ["lib", "spec/support", "spec/factories"]
  defp elixirc_paths(_), do: ["lib"]

  defp deps do
    [{:maru, "~> 0.11.2"},
    {:ecto, "~> 2.1"},
    {:postgrex, "~> 0.13.0"},
    {:vex, "~> 0.6.0"},
    {:timex, "~> 3.0"},
    {:timex_ecto, "~> 3.0"},
    {:json_web_token, "~> 0.2"},
    {:cors_plug, "~> 1.1"},
    {:kafka_ex, "~> 0.6.5"},
    {:avrolixr, git: "https://github.com/retgoat/avrolixr"},
    {:erlavro, git: "https://github.com/avvo/erlavro"},
    {:httpoison, "~> 0.11.1"},
    {:envy, "~> 1.0.0"},
    {:inflex, "~> 1.8.1" },
    {:exsync, "~> 0.1", only: :dev},
    {:espec, "~> 1.3.2", only: :test},
    {:ex_machina, "~> 2.0", only: :test}]
  end
end
