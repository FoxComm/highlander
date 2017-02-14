defmodule Hyperion.Amazon do
  import Ecto.Query
  alias Hyperion.Repo
  alias Hyperion.PhoenixScala.Client

  alias Hyperion.Amazon.TemplateBuilder

  def product_feed(product_id, jwt) do
    # FIXME: get products by may ids
    # remove hd(product_id) and update PhoenixScala.Client
    Client.get_product(hd(product_id), jwt)
    |> process_products
  end

  def process_products(response) do
    products = response.body["attributes"] |> format_map

    skus = response.body["skus"]
           |> Enum.map(fn(map) -> format_map(map["attributes"]) end)
           |> Enum.filter( fn (m) -> m[:channel] == "amazon"  end)

    Enum.map(skus, fn(s) -> Enum.into(s, products) end)
    |> Enum.map(fn list -> Keyword.delete(list, :description, "<p><br></p>") end)
  end

  def price_feed(product_id, jwt) do
    product_feed(product_id, jwt)
    |> Enum.map(fn(x) -> Enum.filter(x, fn({k, v}) -> k == :code || k == :retailprice  end) end)
  end

  defp format_map(map) do
    Enum.map(map, fn({k, %{"t" => _t, "v" => v}}) -> {String.downcase(k) |> String.to_atom, v} end)
    |> Enum.reject(fn({_k, v}) -> v == nil || v == "" end)
  end
end
