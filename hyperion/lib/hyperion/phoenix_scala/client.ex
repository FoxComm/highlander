defmodule Hyperion.PhoenixScala.Client do
  use HTTPoison.Base

  @moduledoc """
  Provides simple access to Phoenix-scala API
  """

  def process_url(path) do
    {:ok, base_uri} = Application.fetch_env(:hyperion, :phoenix_url)
    base_uri <> path
  end

  @doc """
  DO NOT USE IT! This function made just for testing!
  """
  def login do
    {:ok, req} = Poison.encode(%{email: "admin@admin.com", password: "password", org: "tenant"})
    post("/api/v1/public/login", req, request_headers)
    case post("/api/v1/public/login", req, request_headers) do
      {:ok, resp} -> Keyword.take(resp.headers, ["JWT"]) |> hd |> elem(1)
      {:error, err} -> inspect(err)
    end
  end

  @doc """
  Returns product by id
  """
  def get_product(product_id, token, ctx \\ "default") do
    get("/api/v1/products/#{ctx}/#{product_id}", request_headers(token))
    |> parse_response(token)
  end

  # private functions
  defp parse_response({_status, r = %HTTPoison.Response{}}, token) do
    jwt = if token do
            token
          else
            r.headers
            |> Keyword.take(["JWT"])
            |> Enum.map(fn({k, v}) -> {String.to_atom(k), v} end)
            |> hd |> elem(1)
          end
    %{body: r.body, jwt: jwt}
  end

  def request_headers(jwt \\ nil) do
    case jwt == nil do
      true -> ["Content-Type": "application/json"]
      _ -> ["Content-Type": "application/json", "JWT": jwt]
    end
  end

  defp process_response_body(body) do
    case Poison.decode(body) do
      {:ok, body} -> body
      {_, error} -> inspect(error)
    end
  end
end
