defmodule Hyperion.Amazon do
  alias Hyperion.PhoenixScala.Client

  @doc """
  Returns formatted indexed list of products gotten by it's ids
  """
  def product_feed(product_id, jwt) do
    # FIXME: get products by may ids
    # remove hd(product_id) and update PhoenixScala.Client
    Client.get_product(hd(product_id), jwt)
    |> process_products
    |> set_parentage
    |> Enum.with_index(1)
  end

  @doc """
  Returns formatted indexed list of products prices gotten by products ids
  """
  def price_feed(product_id, jwt) do
     Client.get_product(hd(product_id), jwt)
    |> process_products
    |> Enum.map(fn(x) -> Enum.filter(x, fn({k, _v}) -> k == :code || k == :retailprice end) end)
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

  # gets the albums and images for skus,
  # sets the album name as key and atomize it
  # adds the SKU for matching
  defp process_images(response) do
    r = response.body
    albums = Enum.map(r["skus"], fn(sku)->
      Enum.map(sku["albums"], fn(album) ->
        {String.to_atom(album["name"]), album["images"]}
      end)
    end) |> Enum.reject(fn(list) -> list == [] end)

    case albums do
      [] -> raise "No images for product #{r["id"]}"
      x -> Enum.map(r["skus"], fn(x)-> [albums: hd(albums), code: x["attributes"]["code"]["v"]] end)
    end
  end

  # gets product, skus, and variants info
  # merge product with all skus, delete empty :description field
  # add variants to spoper skus
  # sets parent product
  defp process_products(response) do
    raw_products = response.body["attributes"]
               |> format_map

    skus = response.body["skus"]
           |> Enum.map(fn(map) -> format_map(map["attributes"]) end)



    products = Enum.map(skus, fn(s) -> Enum.into(s, raw_products) end)
               |> Enum.map(fn list -> Keyword.delete(list, :description, "<p><br></p>") end)

    # If we have variants — process them
    # if not — return products list
    case response.body["variants"] do
      [] -> products
      _ ->
        variants = process_variants(response.body["variants"])
        for product <- products do
          Enum.into(product, Keyword.get(variants, String.to_atom(product[:code])))
        end
    end
  end

  # atomizes the keys
  # transform variants data to proper structure
  # assign variants options to associated sku
  defp process_variants(variants) do
    func = fn var ->
      {String.to_atom(var["attributes"]["name"]["v"]), atomize_keys(var["values"])}
    end

    props = Enum.map(variants, func)
            |> Enum.flat_map(fn(variant) -> transform_variants(variant) end)
    skus = Enum.map(props, fn{sku, _l} -> sku end) |> Enum.uniq

    for sku <- skus do
      Keyword.put_new([], sku, Keyword.get_values(props, sku) |> List.flatten)
    end |> List.flatten
  end

  # for each sku in list (not unique) gets the all available options as {key, name}
  # groups the list by sku and flatten it
  defp transform_variants({key, var}) do
    for v <- var do
      name = v[:name]
      list = Keyword.new
      for s <- v[:skuCodes] do
        Keyword.put(list, String.to_atom(s), Keyword.put_new([], key, name))
      end
    end |> List.flatten
        |> Enum.group_by(fn({key, _lst}) -> key end)
        |> Map.to_list
        |> Enum.flat_map(fn{_k, data} -> data end)
  end

  # deeply atomize all keys in Keyword structure
  defp atomize_keys(values) do
    for val <- values do
      atomk = fn({k, v}, acc) ->
                Map.put_new(acc, String.to_atom(k), v)
              end
      Enum.reduce(val, %{}, atomk)
    end
  end

  # duplicates first list element
  # removes unneeded fields
  # adds parent SKU
  # adds duplicated element as parent to all children
  defp set_parentage(list) do
    parent = hd(list)
             |> Enum.into(parentage: "parent")
             |> Enum.reject(fn{k, _v} -> k in [:upc, :taxcode] end)
             |> Keyword.update(:code, nil, &("PARENT#{&1}"))
    children = Enum.map(list, fn(c) -> Enum.into(c, parentage: "child") end)
    [parent|children]
  end

  # formats mas from `"foo" => {"t" => 'type', "v" => 'value'}`
  # to foo: "value"
  # removes empty and nil values
  defp format_map(map) do
    Enum.map(map, fn({k, %{"t" => _t, "v" => v}}) -> {format_string(k), v} end)
    |> Enum.reject(fn({_k, v}) -> v == nil || v == "" end)
  end

  defp format_string(str) do
    str
    |> String.downcase
    |> String.replace(~r/\s+/, "")
    |> String.to_atom
  end
end
