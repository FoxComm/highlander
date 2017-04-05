defmodule Hyperion do
  use Application

  def start(_type, _args) do
    import Supervisor.Spec, warn: false

    unless Mix.env == :prod do
      Envy.auto_load
      Envy.reload_config
    end

    children = [
      worker(Hyperion.Repo, []),
      worker(Hyperion.Amazon.Workers.CustomersOrdersWorker, []),
      worker(Hyperion.Amazon.Workers.PushCheckerWorker, [])
    ]

    unless Mix.env == :test do
      Hyperion.PhoenixScala.Client.create_amazon_plugin_in_ashes()
    end

    opts = [strategy: :one_for_one, name: Hyperion.Supervisor]
    Supervisor.start_link(children, opts)
  end
end
