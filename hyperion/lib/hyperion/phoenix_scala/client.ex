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
  Gets JWT from Phoenix. Try to not use it very often because it takes a lot of time
  """
  def login do
    email = Application.fetch_env!(:hyperion, :phoenix_email)
    password = Application.fetch_env!(:hyperion, :phoenix_password)
    org = Application.fetch_env!(:hyperion, :phoenix_org)
    params = Poison.encode!(%{email: email, password: password, org: org})

    case post("/api/v1/public/login", params, make_request_headers()) do
      {_, %{body: _, headers: headers, status_code: 200}} ->
        Keyword.take(headers, ["Jwt"]) |> hd |> elem(1)

      {_, %{body: resp, headers: _, status_code: _}} ->
        raise %PhoenixError{message: hd(resp["errors"])}
    end
  end

  @doc """
  Returns product by id
  """
  def get_product(product_id, token, ctx \\ "default") do
    get("/api/v1/products/#{ctx}/#{product_id}", make_request_headers(token))
    |> parse_response(token)
  end

  @doc """
  Returns sku by SKU-CODE
  """
  def get_sku(sku_code, token, ctx \\ "default") do
    get("/api/v1/skus/#{ctx}/#{sku_code}", make_request_headers(token))
    |> parse_response(token)
  end

  @doc """
  Returns all non archived skus
  """
  def get_all_skus(token, size \\ 50) do
    q = %{query: %{bool: %{filter: [%{missing: %{field: "archivedAt"}}]}}}

    post(
      "/api/search/admin/sku_search_view/_search?size=#{size}",
      Poison.encode!(q),
      make_request_headers(token)
    )
    |> parse_response(token)
  end

  @doc """
  Return all countries from Phoenix
  """
  def get_countries(token) do
    get("/api/v1/public/countries", make_request_headers(token))
    |> parse_response(token)
  end

  @doc """
  Return all countries from Phoenix
  """
  def get_countries do
    token = login()

    get("/api/v1/public/countries", make_request_headers(token))
    |> parse_response(token)
  end

  def get_regions(country_id) do
    token = login()

    get("/api/v1/public/countries/#{country_id}", make_request_headers(token))
    |> parse_response(token)
  end

  def get_regions(country_id, token) do
    get("/api/v1/public/countries/#{country_id}", make_request_headers(token))
    |> parse_response(token)
  end

  @doc """
  Creates new customer in Phoenix from Amazon order
  """
  def create_customer(%{name: name, email: email}) do
    params = Poison.encode!(%{name: name, email: email})
    token = login()
    {st, resp} = post("/api/v1/customers", params, make_request_headers(token))

    case resp.status_code do
      code when code == 200 -> parse_response({st, resp}, token)
      _ -> %{status: resp.status_code, error: resp.body["errors"]}
    end
  end

  @doc """
  Returns MWS credentials stored in Phoenix plugins
  """
  def get_credentials(token) do
    {_, resp} = get("/api/v1/plugins/settings/AmazonMWS/detailed", make_request_headers(token))

    case resp.status_code do
      code when code in [200, 201] ->
        resp.body["settings"]
        |> Enum.reduce(%{}, fn {k, v}, acc -> Map.put(acc, String.to_atom(k), v) end)

      _ ->
        raise %AmazonCredentialsError{}
    end
  end

  @doc """
  Creates Amazon MWS plugin in phoenix on Hyperion start
  """
  def create_amazon_plugin_in_ashes do
    token = login()

    params = %{
      "name" => "AmazonMWS",
      "description" => "Provides access to Amazon Marketplace Web Service (MWS)",
      "version" => "1.0",
      "schemaSettings" => [
        %{
          "default" => "",
          "name" => "seller_id",
          "title" => "Amazon seller ID",
          "type" => "string"
        },
        %{
          "default" => "",
          "name" => "mws_auth_token",
          "title" => "Amazon MWS Auth Token",
          "type" => "string"
        }
      ]
    }

    post("/api/v1/plugins/register", Poison.encode!(params), make_request_headers(token))
    |> parse_response(token)
  end

  # private functions
  defp parse_response({_status, r = %HTTPoison.Response{}}, token) do
    case r.status_code do
      c when c in [200, 201] ->
        jwt =
          if token do
            token
          else
            r.headers
            |> Keyword.take(["JWT"])
            |> Enum.map(fn {k, v} -> {String.to_atom(k), v} end)
            |> hd
            |> elem(1)
          end

        %{body: r.body, jwt: jwt}

      _ ->
        raise %PhoenixError{message: hd(r.body["errors"])}
    end
  end

  defp make_request_headers(jwt \\ nil) do
    case jwt do
      nil -> ["Content-Type": "application/json"]
      _ -> ["Content-Type": "application/json", JWT: jwt]
    end
  end

  defp process_response_body(body) do
    case Poison.decode(body) do
      {:ok, body} -> body
      {_, error} -> inspect(error)
    end
  end
end
