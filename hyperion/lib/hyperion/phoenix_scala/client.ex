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
    params = Poison.encode!(%{email: "admin@admin.com", password: "password", org: "tenant"})
    case post("/api/v1/public/login", params, make_request_headers()) do
      {:ok, resp} -> Keyword.take(resp.headers, ["Jwt"]) |> hd |> elem(1)
      {:error, err} -> inspect(err)
    end
  end

  @doc """
  Returns product by id
  """
  def get_product(product_id, token, ctx \\ "default") do
    get("/api/v1/products/#{ctx}/#{product_id}", make_request_headers(token))
    |> parse_response(token)
  end

  def get_sku(sku_code, token, ctx \\ "default") do
    get("/api/v1/skus/#{ctx}/#{sku_code}", make_request_headers(token))
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


  def make_request_headers(jwt \\ nil) do
    case jwt do
      nil -> ["Content-Type": "application/json"]
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
