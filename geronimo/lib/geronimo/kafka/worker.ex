defmodule Geronimo.Kafka.Worker do
  @moduledoc """
  Starts KafkaEs worker on app start and registers all needed schemas
  NB: Add new modules to register_schemas() if needed.
  """

  require Logger
  alias Geronimo.Kafka.SchemaRegistryClient

  def start do
    kafka_url = [{Application.fetch_env!(:geronimo, :kafka_host),
                  Application.fetch_env!(:geronimo, :kafka_port) |> String.to_integer }]
    KafkaEx.create_worker(:geronimo_worker, [uris: kafka_url,
                          consumer_group: Application.fetch_env!(:geronimo, :consumer_group)])
    register_schemas()
  end

  def register_schemas do
    Task.async(fn->
      modules = [Geronimo.ContentType, Geronimo.Entity]

      Enum.each(modules, fn(module) ->
        key_schema = apply(module, :avro_schema_key, [])
        value_schema = apply(module, :avro_schema_value, [])
        object = apply(module, :table, [])
        {:ok, k_res} = SchemaRegistryClient.store_schema("#{object}-key", key_schema)
        {:ok, v_ver} = SchemaRegistryClient.store_schema("#{object}-value", value_schema)
        Logger.info "Schemas for #{object} registered. Key: #{inspect(k_res)}, value: #{inspect(v_ver)}"
      end)
    end) |> Task.await
  end
end
