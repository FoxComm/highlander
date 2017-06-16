defmodule Geronimo do
  use Application

  def start(_type, _args) do
    import Supervisor.Spec, warn: false

    unless Mix.env == :prod do
      Envy.auto_load
      Envy.reload_config
    end

    children = [
      worker(Geronimo.Repo, [])
    ]
    opts = [strategy: :one_for_one, name: Geronimo.Supervisor]
    Supervisor.start_link(children, opts)
  end
end
