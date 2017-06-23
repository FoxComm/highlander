defmodule Geronimo.Kafka.SchemaRegistryClient do
  @moduledoc """
  Provides a easy and conveniens functions to get/store schemas in schema registry
  """
  use HTTPoison.Base

  def get_schema(object, id) do
    get("/subjects/#{object}/versions/#{id}")
    |> response_body()
  end

  def store_schema(object, schema) do
    post("/subjects/#{object}/versions", Poison.encode!(%{schema: schema}))
    |> response_body()
  end

  defp response_body(response) do
    case response do
      {:ok, %HTTPoison.Response{body: body, headers: _, status_code: 200}} -> {:ok, body}
      {:ok, %HTTPoison.Response{body: body, headers: _, status_code: _}} -> {:error, body}
      {:error, err} -> {:fail, err.reason}
    end
  end

  def process_url(path) do
    ip = Application.fetch_env!(:geronimo, :schema_registry_ip)
    port = Application.fetch_env!(:geronimo, :schema_registry_port)
    "http://#{ip}:#{port}" <> path
  end

  def process_request_headers(), do: ["Content-Type", "application/vnd.schemaregistry.v1+json"]

  def process_response_body(body) do
    body
    |> Poison.decode!
    |> Utils.atomize
  end
end
