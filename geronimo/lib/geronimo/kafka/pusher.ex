defmodule Geronimo.Kafka.Pusher do
  @moduledoc """
  Implements sync and async pushes to Kafka
  """
  require Logger

  def push(kind, obj) do
    res = KafkaEx.produce("geronimo_#{kind}", 0, Poison.encode!(obj),
                          key: "#{kind}_#{obj.id}", worker_name: :geronimo_worker)
    Logger.debug "Pushed to Kafka #{inspect(res)}"
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
