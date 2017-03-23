defmodule Hyperion.Amazon do
  alias Hyperion.PhoenixScala.Client
  alias Hyperion.JwtAuth
  require Logger

  @moduledoc """
  This module responsible for transform data from Phoenix to Amazon and vice versa
  """

  @doc """
  Returns formatted indexed list of products gotten by it's ids
  """
  def product_feed([product_id], jwt) do
    # FIXME: get products by may ids
    # remove hd(product_id) and update PhoenixScala.Client
    Client.get_product(product_id, jwt)
    |> process_products(true)
    |> Enum.with_index(1)
  end

  @doc """
  Formats the given product
  """
  def product_feed(data = %{body: _product}) do
    process_products(data, true)
    |> Enum.with_index(1)
  end

  @doc """
  Returns formatted indexed list of products prices gotten by products ids
  """
  def price_feed([product_id], jwt) do
     Client.get_product(product_id, jwt)
    |> process_products
    |> Enum.map(fn(x) -> Enum.filter(x, fn({k, _v}) -> k == :code || k == :retailprice end) end)
    |> Enum.with_index(1)
  end

  @doc """
  Formats the given price
  """
  def price_feed(data = %{body: _product}) do
    process_products(data)
    |> Enum.map(fn(x) -> Enum.filter(x, fn({k, _v}) -> k == :code || k == :retailprice end) end)
    |> Enum.with_index(1)
  end

  @doc """
  Returns formatted list of products images gotten by products ids
  !important: List is NOT indexed because we need a sequentially number all albums.
  """
  def images_feed([product_id], jwt) do
    Client.get_product(product_id, jwt)
    |> process_images
  end

  @doc """
  Formats the given inventory
  """
  def images_feed(data = %{body: _product}) do
    process_images(data)
  end

  def inventory_feed([product_id], jwt) do
    Client.get_product(product_id, jwt)
    |> process_images
    |> process_inventory
  end

  def inventory_feed(data = %{body: _product}) do
    process_products(data)
    |> process_inventory
  end

  defp process_inventory(products) do
    Enum.flat_map(products, fn(el) -> [%{sku: el[:code], quantity: el[:inventory]}] end)
    |> Enum.with_index(1)
  end

  def get_full_order(order_id) do
    try do
      order_details = get_order_details(order_id)
      order_items = get_order_items(order_id)
      build_order_map(order_details, order_items)
    rescue e ->
      er_name = e.__struct__
      er_name in [AmazonError] -> Logger.error("#{er_name}")
    end
  end

  def build_order_map(order, items) do
    %{
      result: %{
        referenceNumber: order["AmazonOrderId"],
        paymentState: "Captured",
        line_items: %{
          skus: [%{
              name: "foo"
            }
          ]
        },
        lineItemAdjustments: [],
        totals: %{
         subTotal: calculate_amount_for_order(items["OrderItems"]["OrderItem"], "ItemPrice"),
         taxes: calculate_amount_for_order(items["OrderItems"]["OrderItem"], "ItemTax"),
         shipping: calculate_amount_for_order(items["OrderItems"]["OrderItem"], "ShippingPrice"),
         adjustments: 0,
         total: String.to_float(order["OrderTotal"]["Amount"]) * 100 |> round
        },
        customer: %{
          email: order["BuyerEmail"],
          name: order["BuyerName"]
        },
        shippingMethod: %{
          id: 0,
          name: order["ShipmentServiceLevelCategory"],
          code: order["ShipServiceLevel"],
          price: calculate_amount_for_order(items["OrderItems"]["OrderItem"], "ShippingPrice"),
          isEnabled: true
        },
        shippingAddress: %{
          id: 0,
          region: %{
            id: 0,
            countryId: 0,
            name: order["ShippingAddress"]["StateOrRegion"],
          },
          name: order["ShippingAddress"]["Name"],
          address1: order["ShippingAddress"]["AddressLine1"],
          address2: order["ShippingAddress"]["AddressLine2"],
          city: order["ShippingAddress"]["City"],
          zip: order["ShippingAddress"]["PostalCode"],
          isDefault: false
        },
        paymentMethods: %{
          id: 0,
          amount: (String.to_float(order["OrderTotal"]["Amount"]) * 100 |> round),
          currentBalance: 0,
          availableBalance: 0,
          createdAt: order["PurchaseDate"],
          type: order["PaymentMethodDetails"]["PaymentMethodDetail"]
        },
        orderState: order["OrderStatus"],
        shippingState: "---",
        fraudScore: 0,
        placedAt: order["PurchaseDate"],
        channel: order["SalesChannel"]
      }
    }
  end

  def calculate_amount_for_order(order_items, amount_type) when is_map(order_items) do
    String.to_float(order_items[amount_type]["Amount"]) * 100
    |> round
  end

  def calculate_amount_for_order(order_items, amount_type) when is_list(order_items) do
    Enum.reduce(order_items, 0, fn(x, acc) ->
      (String.to_float(x[amount_type]["Amount"]) * 100 |> round) + acc
    end)
  end

  def get_order_details(order_id) do
    case MWSClient.get_order([order_id], fetch_config()) do
      {:error, error} -> raise %AmazonError{message: "Error while fetching order from amazon: #{inspect(error)}"}
      {:warn, warn} -> raise %AmazonError{message: warn["ErrorResponse"]["Error"]["Message"]}
      {_, resp} -> resp["GetOrderResponse"]["GetOrderResult"]["Orders"]["Order"]
    end
  end

  def get_order_items(order_id) do
    case MWSClient.list_order_items(order_id, fetch_config()) do
      {:error, error} -> raise %AmazonError{message: "Error while fetching order details from amazon: #{inspect(error)}"}
      {:warn, warn} -> raise %AmazonError{message: warn["ErrorResponse"]["Error"]["Message"]}
      {_, resp} -> resp["ListOrderItemsResponse"]["ListOrderItemsResult"]
    end
  end

  def fetch_config do
    {_, jwt} = Client.login
               |> JwtAuth.verify
    Credentials.mws_config(jwt.scope)
  end

  # gets the albums and images for skus,
  # adds the SKU for matching
  # renders main, pt and swatch images
  defp process_images(response) do
    r = response.body
    albums = Enum.map(r["skus"], fn(sku)->
      Enum.map(sku["albums"], fn(album) ->
        {String.to_atom(album["name"]), album["images"]}
      end)
    end) |> Enum.reject(fn(list) -> list == [] end)

    case albums do
      [] -> raise "No images for product #{r["id"]}"
      _ -> Enum.map(r["skus"], fn(x)-> [albums: hd(albums), code: x["attributes"]["code"]["v"]] end)
    end
    |> Enum.flat_map(fn(product) ->
        main = render_main_section(hd(product[:albums]), product[:code])
        [main, render_swatch_section(product[:albums][:swatches], product[:code], Enum.count(main))]
       end) |> Enum.reject(fn el -> el == nil end)
  end

  # renders main images section as:
  #  [[{[type: "Main",
  #   location: "http:"], 1},
  # {[type: "PT",
  #   location: "http:", id: 1],
  #   2}],
  defp render_main_section({_, [h|[]]}, sku) do
    [{[sku: sku, type: "Main", location: String.replace(h["src"], "https", "http")], 1}]
  end

  defp render_main_section({_, [h|t]}, sku) do
    main = [sku: sku, type: "Main", location: String.replace(h["src"], "https", "http")]

    pt = Enum.with_index(t, 1)
         |> Enum.flat_map(fn({img, idx}) ->
              [sku: sku, type: "PT", location: String.replace(img["src"], "https", "http"), id: idx]
            end)
    [main, pt]
    |> Enum.with_index(1)
  end

  def render_swatch_section(nil, _, _), do: nil

  # renders swatches section as:
  # [[type: Swatch, lication: http://, id: id]]
  def render_swatch_section(list, sku, initial) do
    Enum.with_index(list, initial + 1)
    |> Enum.map(fn {image, idx} ->
      [sku: sku, type: "Swatch", location: String.replace(image["src"], "https", "http"), idx: idx]
    end)
  end


  # gets product, skus, and variants info
  # merge product with all skus, delete empty :description field
  # adds variants to proper skus
  # sets parent product
  # attaches `item_type' and `department' from the product to each sku
  defp process_products(response, set_parent \\ false) do
    category_data = Category.get_category_data(response.body["attributes"]["nodeId"]["v"])
    raw_products = response.body["attributes"]
                   |> format_map

    skus = response.body["skus"]
           |> Enum.map(fn(map) -> format_map(map["attributes"]) end)



    products = Enum.map(skus, fn(s) -> Enum.into(s, raw_products) end)
               |> Enum.map(fn list -> Keyword.delete(list, :description, "<p><br></p>") end)
               |> Enum.filter(fn el -> el[:amazon] == true end)

    # If we have variants — process them, set parent product, and attach category
    # if not — return products list and attach category
    case response.body["variants"] do
      [] -> products
      _ ->
        variants = process_variants(response.body["variants"])
        lst = for product <- products do
                Enum.into(product, Keyword.get(variants, String.to_atom(product[:code])))
              end
        case set_parent do
          true -> set_parentage(lst)
          _ -> lst
        end
    end
    |> Enum.map(fn(product) -> Enum.into(product, category_data) end)
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

  # formats mas from `"foo" => {"t" => 'type', "v" => 'value'}`
  # to foo: "value"
  # removes empty and nil values
  defp format_map(map) do
    attrs = fn x ->
      case x do
        {k, %{"t" => _t, "v" => v}} -> {format_string(k), v}
        {k, %{"v" => v }} -> {format_string(k), v}
      end
    end
    Enum.map(map, attrs)
    |> Enum.reject(fn({_k, v}) -> v == nil || v == "" end)
  end

  defp format_map(map) when is_list(map), do: []

  defp format_string(str) do
    str
    |> String.downcase
    |> String.replace(~r/\s+/, "")
    |> String.to_atom
  end
end
