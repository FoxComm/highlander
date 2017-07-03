defmodule Geronimo.Kafka.SchemaRegistryClient do
  use HTTPoison.Base

  def get_schema(object, id) do
    case get("/subjects/#{object}/versions/#{id}") do
      {:ok, %HTTPoison.Response{body: body, headers: _, status_code: 200}} ->
        body[:schema]
        |> Poison.decode!
        |> Utils.atomize
      {:ok, %HTTPoison.Response{body: body, headers: _, status_code: _}} -> body
      {:error, err} -> err
    end
  end

  def store_schema(schema) do
    schema
  end

  def process_url(url) do
    "http://#{schema_registry_url()}/#{url}"
  end

  def process_response_body(body) do
    body
    |> Poison.decode!
    |> Enum.map(fn({k, v}) -> {String.to_atom(k), v} end)
  end

  def schema_registry_url do
    url = Application.fetch_env!(:geronimo, :schema_registry_ip)
    port = Application.fetch_env!(:geronimo, :schema_registry_port)
    "http://#{url}:#{port}"
  end

  def process_request_headers(), do: ["Content-Type", "application/vnd.schemaregistry.v1+json"]
end
