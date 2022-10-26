defmodule Geronimo.Kafka.Pusher do
  @moduledoc """
  Implements sync and async pushes to Kafka
  """
  require Logger

  def push(module, obj) do
    kind = apply(module, :table, [])
    data = apply(module, :avro_encode!, [obj])
    res = KafkaEx.produce("geronimo_#{kind}", 0, data,
                          key: "#{kind}_#{obj.id}",
                          worker_name: :geronimo_worker)
    Logger.debug "#{Inflex.singularize(kind)} with id #{obj.id} pushed to kafka #{inspect(res)}"
  end

  def push_async(kind, obj) do
    unless Mix.env == :test do
      Task.async(fn ->
        push(kind, obj)
      end)
    end
  end

  def push_async_await(kind, obj) do
    push_async(kind, obj) |> Task.await
  end
end
