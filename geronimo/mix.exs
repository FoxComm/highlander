defmodule Geronimo.Mixfile do
  use Mix.Project

  def project do
    [app: :geronimo,
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
    [extra_applications: (Mix.env == :dev && [:exsync] || []) ++ [:logger, :maru, :timex, :ecto, :postgrex,
                                                                  :timex_ecto, :kafka_ex, :avrolixr, :erlavro],
     mod: {Geronimo, []}]

  end

  defp deps do
    [{:ecto, "~> 2.1"},
    {:postgrex, "~> 0.13.0"},
    {:maru, "~> 0.11"},
    {:vex, "~> 0.6.0"},
    {:timex, "~> 3.0"},
    {:timex_ecto, "~> 3.0"},
    {:json_web_token, "~> 0.2"},
    {:cors_plug, "~> 1.1"},
    {:kafka_ex, "~> 0.6.5"},
    {:avrolixr, git: "https://github.com/retgoat/avrolixr"},
    {:erlavro, git: "https://github.com/avvo/erlavro"},
    {:envy, "~> 1.1.1"},
    {:inflex, "~> 1.8.1" },
    {:exsync, "~> 0.1", only: :dev}]
  end
end
