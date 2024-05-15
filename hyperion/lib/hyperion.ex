defmodule Hyperion do
  use Application

  def start(_type, _args) do
    import Supervisor.Spec, warn: false

    unless Mix.env == :prod do
      Envy.auto_load
      Envy.reload_config
    end

    # Create plugin on app start only if ENV var defined
    # Pattern match against `True' because of marathon
    case Application.fetch_env(:hyperion, :create_plugin) do
      {:ok, "true"} -> Hyperion.PhoenixScala.Client.create_amazon_plugin_in_ashes()
      _ -> nil
    end

    children = [
      worker(Hyperion.Repo, []),
      worker(Hyperion.Amazon.Workers.CustomersOrdersWorker, []),
      worker(Hyperion.Amazon.Workers.PushCheckerWorker, []),
      worker(Hyperion.MWSAuth, [])
    ]


    opts = [strategy: :one_for_one, name: Hyperion.Supervisor]
    Supervisor.start_link(children, opts)
  end
end
