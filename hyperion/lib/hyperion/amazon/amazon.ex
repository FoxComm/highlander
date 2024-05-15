defmodule Hyperion.Amazon do
  alias Hyperion.PhoenixScala.Client, warn: true
  alias Hyperion.Amazon.Credentials, warn: true
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

  def fetch_config do
    token = Client.login
    fetch_config(token)
  end

  def fetch_config(token) do
    Credentials.mws_config(token)
  end

  def safe_fetch_config() do
    case Client.safe_login() do
      token -> Credentials.safe_mws_config(token)
      nil -> nil
    end
  end

  def get_full_order(order_id, token) do
    try do
      cfg = fetch_config(token)
      order = get_order_details(order_id, cfg)
      items = get_order_items(order_id, cfg)
      build_order_map(order,
                      items["OrderItems"]["OrderItem"], token)
    rescue e ->
      er_name = e.__struct__
      Logger.error("#{er_name} #{e.message}")
      reraise e, System.stacktrace
    end
  end

  defp process_inventory(products) do
    Enum.flat_map(products, fn(el) -> [%{sku: el[:code], quantity: el[:inventory]}] end)
    |> Enum.with_index(1)
  end


  defp build_order_map(order, items) when order == %{} and items == %{}, do: %{}

  defp build_order_map(order, items, token) do
    %{
      result: %{
        referenceNumber: order["AmazonOrderId"],
        paymentState: "Captured",
        lineItems: %{
          skus: get_sku_map(items, token),
        },
        lineItemAdjustments: [],
        totals: %{
         subTotal: calculate_amount_for_order(items, "ItemPrice"),
         taxes: calculate_amount_for_order(items, "ItemTax"),
         shipping: calculate_amount_for_order(items, "ShippingPrice"),
         adjustments: 0,
         total: String.to_float(order["OrderTotal"]["Amount"]) * 100 |> round
        },
        customer: Client.get_customer_by_email(order["BuyerEmail"], token).body,
        shippingMethod: %{
          id: 0,
          name: order["ShipmentServiceLevelCategory"],
          code: order["ShipServiceLevel"],
          price: calculate_amount_for_order(items, "ShippingPrice"),
          isEnabled: true
        },
        shippingAddress: %{
          id: 0,
          region: %{
            id: 0,
            countryId: get_country_id(order["ShippingAddress"]["CountryCode"], token),
            name: order["ShippingAddress"]["StateOrRegion"],
          },
          name: order["ShippingAddress"]["Name"],
          address1: order["ShippingAddress"]["AddressLine1"],
          address2: order["ShippingAddress"]["AddressLine2"],
          city: order["ShippingAddress"]["City"],
          zip: order["ShippingAddress"]["PostalCode"],
          isDefault: false
        },
        paymentMethods: [%{
          id: 0,
          amount: (String.to_float(order["OrderTotal"]["Amount"]) * 100 |> round),
          currentBalance: 0,
          availableBalance: 0,
          createdAt: order["PurchaseDate"],
          type: lower_first(order["PaymentMethodDetails"]["PaymentMethodDetail"])
        }],
        orderState: order["OrderStatus"],
        shippingState: "---",
        fraudScore: 0,
        placedAt: order["PurchaseDate"],
        channel: order["SalesChannel"]
      }
    }
  end

  defp calculate_amount_for_order(order_items, amount_type) when is_map(order_items) do
    String.to_float(order_items[amount_type]["Amount"]) * 100
    |> round
  end

  defp calculate_amount_for_order(order_items, amount_type) when is_list(order_items) do
    Enum.reduce(order_items, 0, fn(x, acc) ->
      (String.to_float(x[amount_type]["Amount"]) * 100 |> round) + acc
    end)
  end

  def get_order_details(order_id, cfg) do
    case MWSClient.get_order([order_id], cfg) do
      {:error, error} -> raise %AmazonError{message: "Error while fetching order from amazon: #{inspect(error)}"}
      {:warn, warn} -> raise %AmazonError{message: warn["ErrorResponse"]["Error"]["Message"]}
      {_, resp} -> resp["GetOrderResponse"]["GetOrderResult"]["Orders"]["Order"]
    end
  end

  def get_order_items(order_id, cfg) do
    case MWSClient.list_order_items(order_id, cfg) do
      {:error, error} -> raise %AmazonError{message: "Error while fetching order details from amazon: #{inspect(error)}"}
      {:warn, warn} -> raise %AmazonError{message: warn["ErrorResponse"]["Error"]["Message"]}
      {_, resp} -> resp["ListOrderItemsResponse"]["ListOrderItemsResult"]
    end
  end

  defp get_sku_map(items, token) when is_list(items) do
    Enum.map(items, fn item ->
      %{
        imagePath: get_sku_image(item["SellerSKU"], token),
        referenceNumbers: [],
        name: "---",
        sku: items["SellerSKU"],
        price: (String.to_float(item["ItemPrice"]["Amount"]) * 100 |> round),
        quantity: String.to_integer(item["QuantityOrdered"]),
        totalPrice: (String.to_float(item["ItemPrice"]["Amount"]) * 100 |> round) * String.to_integer(item["QuantityOrdered"]),
        productFormId: nil,
        trackInventory: false,
        state: "pending"
      }
    end)
  end

  defp get_sku_map(items, token) when is_map(items) do
    [%{
      imagePath: get_sku_image(items["SellerSKU"], token),
      referenceNumbers: [],
      name: "---",
      sku: items["SellerSKU"],
      price: (String.to_float(items["ItemPrice"]["Amount"]) * 100 |> round),
      quantity: String.to_integer(items["QuantityOrdered"]),
      totalPrice: (String.to_float(items["ItemPrice"]["Amount"]) * 100 |> round) * String.to_integer(items["QuantityOrdered"]),
      productFormId: nil,
      trackInventory: false,
      state: "pending"
    }]
  end

  def get_sku_image(sku, token) do
    try do
      sku = Client.get_sku(sku, token)
      case sku.body["errors"] do
        nil ->
          first_image = hd(sku.body["albums"])["images"] |> hd
          first_image["src"]
        err -> raise %AmazonError{message: "#{err}"}
      end
    rescue _e in PhoenixError ->
      nil
    end
  end

  defp get_country_id(code, token) do
    resp = Client.get_countries(token)
    Enum.filter(resp.body, fn(cnt) -> cnt["alpha2"] == code end)
    |> hd
    |> get_in(["id"])
  end



  # Gets the albums and images for amazon enabled skus,
  # adds the SKU for matching
  # renders main, pt and swatch images
  defp process_images(response) do
    r = response.body
    amazon_skus = Enum.filter(r["skus"], fn (sku) ->
                    sku["attributes"]["amazon"]["v"] == true
                  end)

    case amazon_skus do
      [] -> raise "No images for product #{r["id"]}"
      _ -> amazon_skus
    end
    |> Enum.map(fn(sku)->
      images = Enum.map(sku["albums"], fn(album) ->
        {String.to_atom(album["name"]), album["images"]}
      end)
      [albums: images, code: sku["attributes"]["code"]["v"]]
    end)
    |> Enum.map(fn(sku) ->
        main = render_main_section(hd(sku[:albums]), sku[:code], 1)
        [main, render_swatch_section(sku[:albums][:swatches], sku[:code], Enum.count(main))]
       end)
    |> List.flatten |> Enum.reject(fn el -> el == nil end) |> Enum.with_index(1)
  end

  # renders main images section as:
  #  [[{[type: "Main",
  #   location: "http:"], 1},
  # {[type: "PT",
  #   location: "http:", id: 1],
  #   2}],
  defp render_main_section({_, [h|t]}, sku, _idx) when t == [] do
    IO.puts("t is []")
    [{sku, "Main", String.replace(h["src"], "https", "http")}]
  end

  defp render_main_section({_, [h|t]}, sku, _idx) do
    main = {sku, "Main", String.replace(h["src"], "https", "http")}

    pt = Enum.with_index(t, 1)
         |> Enum.map(fn({img, idx}) ->
              {sku, "PT", String.replace(img["src"], "https", "http"), idx}
            end)
    [main, pt]
    # |> Enum.with_index(idx)
  end

  def render_swatch_section(nil, _, _), do: nil

  # renders swatches section as:
  # [[type: Swatch, lication: http://, id: id]]
  def render_swatch_section(list, sku, initial) do
    Enum.with_index(list, initial + 1)
    |> Enum.map(fn {image, _idx} ->
      {sku, "Swatch", String.replace(image["src"], "https", "http")}
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
      {format_string(var["attributes"]["name"]["v"]), atomize_keys(var["values"])}
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

  defp format_string(str) do
    str
    |> String.downcase
    |> String.replace(~r/\s+/, "")
    |> String.to_atom
  end

  defp lower_first(str) do
    str
    |> String.first
    |> String.downcase
    |> Kernel.<>(String.slice(str, 1, String.length(str)))
  end
end
