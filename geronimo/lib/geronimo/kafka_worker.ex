defmodule Geronimo.KafkaWorker do
  require Logger

  def push(kind, obj) do
    KafkaEx.produce("geronimo_#{kind}", 0, Poison.encode!(obj),
                    key: "#{kind}_#{obj.id}", worker_name: :geronimo_worker)
  end

  def push_async(kind, obj) do
    Task.async(fn ->
      push(kind, obj)
    end)
  end

  def push_async_await(kind, obj) do
    push_async(kind, obj) |> Task.await
  end

end
