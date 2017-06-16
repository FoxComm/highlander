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

    case Application.fetch_env(:geronimo, :start_kafka_worker) do
      {"ok", true} -> start_kafka_ex()
      _ -> nil
    end
  end

  def start_kafka_ex do
    kafka_url = [{Application.fetch_env!(:geronimo, :kafka_host),
                  Application.fetch_env!(:geronimo, :kafka_port) |> String.to_integer }]

    KafkaEx.create_worker(:geronimo_worker, [uris: kafka_url,
                                             consumer_group: Application.fetch_env!(:geronimo, :consumer_group)])
  end
end
