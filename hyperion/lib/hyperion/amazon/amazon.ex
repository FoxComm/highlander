defmodule Hyperion.Amazon do
  import Ecto.Query
  alias Hyperion.Repo
  alias Hyperion.PhoenixScala.Client

  alias Hyperion.Amazon.TemplateBuilder

  @doc """
  Returns formatted indexed list of products gotten by it's ids
  """
  def product_feed(product_id, jwt) do
    # FIXME: get products by may ids
    # remove hd(product_id) and update PhoenixScala.Client
    r = Client.get_product(hd(product_id), jwt)
    |> process_products
    |> Enum.with_index(1)
  end

  @doc """
  Returns formatted indexed list of products prices gotten by products ids
  """
  def price_feed(product_id, jwt) do
     Client.get_product(hd(product_id), jwt)
    |> process_products
    |> Enum.map(fn(x) -> Enum.filter(x, fn({k, v}) -> k == :code || k == :retailprice  end) end)
    |> Enum.with_index(1)
  end

  @doc """
  Returns formatted list of products images gotten by products ids
  !important: List is NOT indexed because we need a sequentially number all albums.
  """
  def images_feed(product_id, jwt) do
    Client.get_product(hd(product_id), jwt)
    |> process_images
  end

  defp process_images(response) do
    r = response.body
    a = Enum.map(r["skus"], fn(s)->
      Enum.map(s["albums"], fn(a) ->
        {String.to_atom(a["name"]), a["images"]}
      end)
    end) |> hd
    Enum.map(r["skus"], fn(x)-> [albums: a, code: x["attributes"]["code"]["v"]] end)
  end

  @doc """
  Construct products list from client result
  """
  defp process_products(response) do
    products = response.body["attributes"] |> format_map

    skus = response.body["skus"]
           |> Enum.map(fn(map) -> format_map(map["attributes"]) end)
           |> Enum.filter( fn (m) -> m[:channel] == "amazon"  end)

    Enum.map(skus, fn(s) -> Enum.into(s, products) end)
    |> Enum.map(fn list -> Keyword.delete(list, :description, "<p><br></p>") end)
  end

  @doc """
  Formats list: convert names to atoms, reduce the {type, value tuple}, and removes empty values
  """
  defp format_map(map) do
    Enum.map(map, fn({k, %{"t" => _t, "v" => v}}) -> {String.downcase(k) |> String.to_atom, v} end)
    |> Enum.reject(fn({_k, v}) -> v == nil || v == "" end)
  end
end
