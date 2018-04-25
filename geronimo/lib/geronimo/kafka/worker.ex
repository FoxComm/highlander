defmodule Geronimo.Kafka.Worker do
  require Logger

  def start do
    kafka_url = [
      {Application.fetch_env!(:geronimo, :kafka_host),
       Application.fetch_env!(:geronimo, :kafka_port) |> String.to_integer()}
    ]

    KafkaEx.create_worker(
      :geronimo_worker,
      uris: kafka_url,
      consumer_group: Application.fetch_env!(:geronimo, :consumer_group)
    )
  end

  def push(kind, obj) do
    KafkaEx.produce(
      "geronimo_#{kind}",
      0,
      Poison.encode!(obj),
      key: "#{kind}_#{obj.id}",
      worker_name: :geronimo_worker
    )
  end

  def push_async(kind, obj) do
    unless Mix.env() == :test do
      Task.async(fn ->
        push(kind, obj)
      end)
    end
  end

  def push_async_await(kind, obj) do
    push_async(kind, obj) |> Task.await()
  end
end
