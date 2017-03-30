defmodule Hyperion do
  use Application

  def start(_type, _args) do
    import Supervisor.Spec, warn: false

    children = [
      worker(Hyperion.Repo, []),
      worker(Hyperion.Amazon.Workers.CustomersOrdersWorker, []),
      worker(Hyperion.Amazon.Workers.PushCheckerWorker, [])
    ]

    opts = [strategy: :one_for_one, name: Hyperion.Supervisor]
    Supervisor.start_link(children, opts)
  end
end
